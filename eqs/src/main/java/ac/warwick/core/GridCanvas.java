package ac.warwick.core;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.sun.jna.Native;
import io.github.libsdl4j.api.rect.SDL_Rect;
import io.github.libsdl4j.api.render.SDL_Texture;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_BGRA8888;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.SDL_TEXTUREACCESS_STATIC;
import static io.github.libsdl4j.api.render.SdlRender.*;

/**
 * A grid of coloured cells rendered as a single texture. Each cell holds an integer value that
 * indexes into a palette of ARGB colours. The texture is re-rasterised lazily, only when a cell
 * changed since the last frame.
 *
 * This class is purely a view: it knows nothing about materials or painting rules. Hit-testing
 * helpers ({@link #rowAt}, {@link #colAt}) let the owning screen translate mouse positions into cells.
 */
public class GridCanvas extends Renderable {
    private final int rows;
    private final int cols;
    private final int cellPx;
    private final int widthPx;
    private final int heightPx;
    private final int[] palette;
    private final int gridLineArgb;
    private final boolean showGrid;

    private final int[][] cells;
    private final SDL_Rect rect;
    private final ByteBuffer pixels;
    private SDL_Texture texture;
    private boolean dirty = true;

    /**
     * @param paletteArgb  colour for each cell value; index 0 is the "empty" colour
     * @param gridLineArgb colour of 1px cell outlines; pass 0 (fully transparent) for none
     */
    public GridCanvas(Renderer renderer, int x, int y, int rows, int cols, int cellPx,
                      int[] paletteArgb, int gridLineArgb) {
        if (rows <= 0 || cols <= 0 || cellPx <= 0) {
            throw new IllegalArgumentException("rows, cols and cellPx must be positive");
        }
        if (paletteArgb == null || paletteArgb.length == 0) {
            throw new IllegalArgumentException("palette must contain at least one colour");
        }
        this.rows = rows;
        this.cols = cols;
        this.cellPx = cellPx;
        this.widthPx = cols * cellPx;
        this.heightPx = rows * cellPx;
        this.palette = paletteArgb.clone();
        this.gridLineArgb = gridLineArgb;
        this.showGrid = (gridLineArgb >>> 24) != 0;

        this.cells = new int[rows][cols];
        this.rect = new SDL_Rect();
        this.rect.x = x;
        this.rect.y = y;
        this.rect.w = widthPx;
        this.rect.h = heightPx;

        this.pixels = ByteBuffer.allocateDirect(widthPx * heightPx * 4);
        this.texture = SDL_CreateTexture(renderer.get_renderer(), SDL_PIXELFORMAT_BGRA8888,
                SDL_TEXTUREACCESS_STATIC, widthPx, heightPx);
        if (texture == null) {
            throw new IllegalStateException("SDL_CreateTexture failed for GridCanvas");
        }
    }

    // ---- hit testing ----------------------------------------------------------------------

    public boolean contains(int px, int py) {
        return px >= rect.x && px < rect.x + widthPx && py >= rect.y && py < rect.y + heightPx;
    }

    /** @return the row under window y-coordinate {@code py}, or -1 if outside the canvas */
    public int rowAt(int py) {
        if (py < rect.y || py >= rect.y + heightPx) return -1;
        return (py - rect.y) / cellPx;
    }

    /** @return the column under window x-coordinate {@code px}, or -1 if outside the canvas */
    public int colAt(int px) {
        if (px < rect.x || px >= rect.x + widthPx) return -1;
        return (px - rect.x) / cellPx;
    }

    // ---- cell access ----------------------------------------------------------------------

    public int get(int row, int col) {
        checkCell(row, col);
        return cells[row][col];
    }

    public void set(int row, int col, int value) {
        checkCell(row, col);
        if (value < 0 || value >= palette.length) {
            throw new IllegalArgumentException("Cell value " + value + " has no palette entry");
        }
        if (cells[row][col] != value) {
            cells[row][col] = value;
            dirty = true;
        }
    }

    public void setAll(int[][] values) {
        if (values.length != rows) throw new IllegalArgumentException("Expected " + rows + " rows");
        for (int r = 0; r < rows; r++) {
            if (values[r].length != cols) throw new IllegalArgumentException("Expected " + cols + " columns");
            for (int c = 0; c < cols; c++) set(r, c, values[r][c]);
        }
    }

    public void clear() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) cells[r][c] = 0;
        }
        dirty = true;
    }

    public int[][] snapshot() {
        int[][] copy = new int[rows][];
        for (int r = 0; r < rows; r++) copy[r] = cells[r].clone();
        return copy;
    }

    public int getRows()   { return rows; }
    public int getCols()   { return cols; }
    public int getX()      { return rect.x; }
    public int getY()      { return rect.y; }
    public int getWidth()  { return widthPx; }
    public int getHeight() { return heightPx; }

    // ---- Renderable -----------------------------------------------------------------------

    public void render(Renderer renderer) {
        if (dirty) {
            rasterise();
            SDL_UpdateTexture(texture, null, Native.getDirectBufferPointer(pixels), widthPx * 4);
            dirty = false;
        }
        SDL_RenderCopy(renderer.get_renderer(), texture, null, rect);
    }

    public void update() { }

    public void destroy() {
        if (texture != null) {
            SDL_DestroyTexture(texture);
            texture = null;
        }
    }

    // ---- internals ------------------------------------------------------------------------

    private void rasterise() {
        IntBuffer out = pixels.asIntBuffer();      // same big-endian ARGB convention as Label
        for (int py = 0; py < heightPx; py++) {
            int row = py / cellPx;
            boolean rowLine = showGrid && (py % cellPx == 0 || py == heightPx - 1);
            int base = py * widthPx;
            for (int px = 0; px < widthPx; px++) {
                int argb;
                if (rowLine || (showGrid && (px % cellPx == 0 || px == widthPx - 1))) {
                    argb = gridLineArgb;
                } else {
                    argb = palette[cells[row][px / cellPx]];
                }
                out.put(base + px, argb);
            }
        }
    }

    private void checkCell(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException("Cell (" + row + ", " + col + ") is outside the "
                    + rows + "x" + cols + " grid");
        }
    }
}