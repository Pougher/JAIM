package ac.warwick.drawing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Single entry point for persisting and reloading a drawing. A submit produces four files in the
 * drawings directory, all written atomically:
 * <pre>
 *   building_&lt;timestamp&gt;.csv   archival copy of the grid
 *   building_&lt;timestamp&gt;.png   same grid, 1 px per cell
 *   latest.csv                 the grid most recently submitted
 *   latest.png                 its image form - the WFC input
 * </pre>
 * On load the CSV is authoritative: the PNG is validated against it and regenerated if it is
 * missing, unreadable, the wrong size, or contains unknown colours.
 */
public final class DrawingStore {

    /** Paths produced by a successful {@link #save}. */
    public static final class Saved {
        public final Path csv;
        public final Path png;
        public final Path latestCsv;
        public final Path latestPng;

        Saved(Path csv, Path png, Path latestCsv, Path latestPng) {
            this.csv = csv;
            this.png = png;
            this.latestCsv = latestCsv;
            this.latestPng = latestPng;
        }
    }

    /** A validated grid together with the image the WFC should consume. */
    public static final class Loaded {
        public final BuildingGrid grid;
        public final BufferedImage image;
        public final Path csvPath;
        public final Path pngPath;
        /** True if latest.png had to be rebuilt from the CSV. */
        public final boolean pngRepaired;

        Loaded(BuildingGrid grid, BufferedImage image, Path csvPath, Path pngPath, boolean pngRepaired) {
            this.grid = grid;
            this.image = image;
            this.csvPath = csvPath;
            this.pngPath = pngPath;
            this.pngRepaired = pngRepaired;
        }
    }

    private DrawingStore() { }

    /** Writes all four files. Partial failure leaves no truncated files behind. */
    public static Saved save(BuildingGrid grid, Path dir) throws IOException {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(dir, "dir");

        Path csv = GridCsv.save(grid, dir);                   // timestamped + latest.csv
        Path png = GridImages.savePngAlongside(grid, csv);    // timestamped .png
        Path latestPng = GridImages.saveLatestPng(grid, dir); // latest.png
        return new Saved(csv, png, GridCsv.latest(dir), latestPng);
    }

    /** Loads the most recent drawing; see class docs for the repair behaviour. */
    public static Loaded loadLatest(Path dir) throws IOException {
        Objects.requireNonNull(dir, "dir");
        Path csvPath = GridCsv.latest(dir);
        Path pngPath = GridImages.latestPng(dir);

        BuildingGrid grid = GridCsv.load(csvPath);            // throws with a precise message if bad
        BufferedImage expected = GridImages.render(grid, 1);

        boolean repaired = false;
        BufferedImage image = null;
        if (Files.isRegularFile(pngPath)) {
            try {
                image = GridImages.loadPng(pngPath);
                if (!GridImages.sameRgb(image, expected)) {
                    System.err.println("[DrawingStore] " + pngPath + " does not match latest.csv; regenerating");
                    image = null;
                }
            } catch (IOException e) {
                System.err.println("[DrawingStore] " + pngPath + " unreadable (" + e.getMessage() + "); regenerating");
            }
        }
        if (image == null) {
            GridImages.saveLatestPng(grid, dir);
            image = expected;
            repaired = true;
        }
        return new Loaded(grid, image, csvPath, pngPath, repaired);
    }
}