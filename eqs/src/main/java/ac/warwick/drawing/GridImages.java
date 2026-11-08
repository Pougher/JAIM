package ac.warwick.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Converts a {@link BuildingGrid} to images, for on-screen previews, PNG export and WFC input. */
public final class GridImages {
    public static final String LATEST_PNG = "latest.png";
    private static final long MAX_PNG_BYTES = 256 * 1024;   // a 24x24 PNG is a few hundred bytes

    private GridImages() { }

    public static Path latestPng(Path dir) {
        return dir.resolve(LATEST_PNG);
    }

    /** Renders the grid at {@code cellPx} pixels per cell with no grid lines. */
    public static BufferedImage render(BuildingGrid grid, int cellPx) {
        return render(grid, cellPx, -1);
    }

    /**
     * Renders the grid at {@code cellPx} pixels per cell.
     * @param gridLineRgb RGB of 1px cell outlines, or a negative value for none
     */
    public static BufferedImage render(BuildingGrid grid, int cellPx, int gridLineRgb) {
        if (cellPx <= 0) throw new IllegalArgumentException("cellPx must be positive");
        int size = BuildingGrid.SIZE * cellPx;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            for (int r = 0; r < BuildingGrid.SIZE; r++) {
                for (int c = 0; c < BuildingGrid.SIZE; c++) {
                    g.setColor(new Color(Material.rgbOf(grid.get(r, c))));
                    g.fillRect(c * cellPx, r * cellPx, cellPx, cellPx);
                }
            }
            if (gridLineRgb >= 0 && cellPx > 2) {
                g.setColor(new Color(gridLineRgb));
                for (int i = 0; i <= BuildingGrid.SIZE; i++) {
                    int p = Math.min(i * cellPx, size - 1);
                    g.drawLine(p, 0, p, size - 1);
                    g.drawLine(0, p, size - 1, p);
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Writes a 1-pixel-per-cell PNG next to a saved CSV (same base name), atomically. */
    public static Path savePngAlongside(BuildingGrid grid, Path csvPath) throws IOException {
        String name = csvPath.getFileName().toString();
        if (name.endsWith(".csv")) name = name.substring(0, name.length() - 4);
        Path png = csvPath.resolveSibling(name + ".png");
        writePng(grid, png);
        return png;
    }

    /** Writes/overwrites {@code latest.png} in {@code dir}, atomically. */
    public static Path saveLatestPng(BuildingGrid grid, Path dir) throws IOException {
        Path png = latestPng(dir);
        writePng(grid, png);
        return png;
    }

    /**
     * Loads a PNG and checks it is a plausible drawing: decodable, exactly 24x24, and every pixel
     * is white or one of the six material colours.
     */
    public static BufferedImage loadPng(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new NoSuchFileException(file.toString());
        if (Files.size(file) > MAX_PNG_BYTES) throw new IOException(file + " is too large to be a drawing");

        BufferedImage img = ImageIO.read(file.toFile());
        if (img == null) throw new IOException(file + " is not a decodable image");
        if (img.getWidth() != BuildingGrid.SIZE || img.getHeight() != BuildingGrid.SIZE) {
            throw new IOException(file + " is " + img.getWidth() + "x" + img.getHeight()
                    + ", expected " + BuildingGrid.SIZE + "x" + BuildingGrid.SIZE);
        }
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                if (!isKnownColour(rgb)) {
                    throw new IOException(file + String.format(": pixel (%d,%d) has unknown colour #%06X", x, y, rgb));
                }
            }
        }
        return toRgb(img);   // normalise the colour model so downstream code sees TYPE_INT_RGB
    }

    /** Pixel-for-pixel RGB equality (alpha ignored). */
    public static boolean sameRgb(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if ((a.getRGB(x, y) & 0xFFFFFF) != (b.getRGB(x, y) & 0xFFFFFF)) return false;
            }
        }
        return true;
    }

    // ---- internals ------------------------------------------------------------------------

    private static void writePng(BuildingGrid grid, Path target) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
        if (!ImageIO.write(render(grid, 1), "png", bytes)) {
            throw new IOException("No PNG image writer available");
        }
        AtomicFiles.write(target, bytes.toByteArray());
    }

    private static boolean isKnownColour(int rgb) {
        if (rgb == Material.EMPTY_RGB) return true;
        for (Material m : Material.values()) {
            if (m.rgb() == rgb) return true;
        }
        return false;
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}