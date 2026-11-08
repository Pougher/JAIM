package ac.warwick.drawing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Persists a {@link BuildingGrid} as plain CSV: 24 lines of 24 comma-separated integers (0–6),
 * no header, so it loads directly into spreadsheets, pandas, numpy, etc.
 *
 * <p>Each save writes a timestamped file plus {@code latest.csv}, both via temp-file + atomic
 * rename so a crash mid-write can never leave a truncated file behind. Loading validates the
 * shape and every value and reports problems with line numbers.</p>
 *
 * <p>The output directory defaults to {@code data/drawings} (relative to the working directory)
 * and can be overridden with {@code -Deqs.dataDir=/some/path}.</p>
 */
public final class GridCsv {
    public static final String LATEST_FILE = "latest.csv";

    private static final String DIR_PROPERTY = "eqs.dataDir";
    private static final String DEFAULT_DIR  = "data/drawings";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final long MAX_FILE_BYTES = 64 * 1024;   // a valid file is ~1.2 KB; reject anything absurd

    private GridCsv() { }

    public static Path defaultDirectory() {
        return Paths.get(System.getProperty(DIR_PROPERTY, DEFAULT_DIR));
    }

    public static Path latest(Path dir) {
        return dir.resolve(LATEST_FILE);
    }

    /**
     * Saves {@code grid} into {@code dir} as {@code building_<timestamp>.csv} and refreshes
     * {@code latest.csv}.
     * @return the path of the timestamped file
     */
    public static Path save(BuildingGrid grid, Path dir) throws IOException {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(dir, "dir");

        Files.createDirectories(dir);
        byte[] bytes = toCsv(grid).getBytes(StandardCharsets.UTF_8);

        Path target = uniquePath(dir);
        AtomicFiles.write(target, bytes);
        AtomicFiles.write(latest(dir), bytes);
        return target;
    }

    /** Loads and validates a grid. Any structural problem is reported as an {@link IOException}. */
    public static BuildingGrid load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) {
            throw new NoSuchFileException(file.toString());
        }
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException(file + " is too large to be a building grid");
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int[][] cells = new int[BuildingGrid.SIZE][BuildingGrid.SIZE];
        int row = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;    // tolerate blank/comment lines
            if (row >= BuildingGrid.SIZE) {
                throw new IOException(file + ": more than " + BuildingGrid.SIZE + " data rows");
            }

            String[] parts = line.split(",", -1);
            if (parts.length != BuildingGrid.SIZE) {
                throw new IOException(file + " line " + (i + 1) + ": expected " + BuildingGrid.SIZE
                        + " values, found " + parts.length);
            }
            for (int c = 0; c < BuildingGrid.SIZE; c++) {
                String token = parts[c].trim();
                int value;
                try {
                    value = Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    throw new IOException(file + " line " + (i + 1) + ": '" + token + "' is not an integer", e);
                }
                if (!Material.isValidValue(value)) {
                    throw new IOException(file + " line " + (i + 1) + ": value " + value
                            + " is outside 0.." + Material.MAX_ID);
                }
                cells[row][c] = value;
            }
            row++;
        }

        if (row != BuildingGrid.SIZE) {
            throw new IOException(file + ": expected " + BuildingGrid.SIZE + " data rows, found " + row);
        }
        return BuildingGrid.fromCells(cells);
    }

    static String toCsv(BuildingGrid grid) {
        int[][] cells = grid.toArray();
        StringBuilder sb = new StringBuilder(BuildingGrid.SIZE * BuildingGrid.SIZE * 2);
        for (int[] row : cells) {
            for (int c = 0; c < row.length; c++) {
                if (c > 0) sb.append(',');
                sb.append(row[c]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static Path uniquePath(Path dir) {
        String stamp = LocalDateTime.now().format(STAMP);
        Path candidate = dir.resolve("building_" + stamp + ".csv");
        for (int n = 1; Files.exists(candidate); n++) {
            candidate = dir.resolve("building_" + stamp + "_" + n + ".csv");
        }
        return candidate;
    }
}