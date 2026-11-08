package ac.warwick.drawing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Converts a {@link BuildingGrid} to images, for on-screen previews and PNG export. */
public final class GridImages {
    private GridImages() { }

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

    /**
     * Writes a 1-pixel-per-cell PNG next to a saved CSV (same base name), atomically.
     * Useful as a direct input for image-based WFC tooling.
     */
    public static Path savePngAlongside(BuildingGrid grid, Path csvPath) throws IOException {
        String name = csvPath.getFileName().toString();
        if (name.endsWith(".csv")) name = name.substring(0, name.length() - 4);
        Path png = csvPath.resolveSibling(name + ".png");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
        if (!ImageIO.write(render(grid, 1), "png", bytes)) {
            throw new IOException("No PNG image writer available");
        }
        AtomicFiles.write(png, bytes.toByteArray());
        return png;
    }
}