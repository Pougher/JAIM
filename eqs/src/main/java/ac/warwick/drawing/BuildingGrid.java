package ac.warwick.drawing;

/**
 * The 24x24 building model. Every cell is {@link Material#EMPTY} or a material id.
 *
 * Rules (enforced by {@link #paint}):
 * <ul>
 *   <li>a material may only be placed on an empty cell – never on top of another material;</li>
 *   <li>the eraser ({@code tool == EMPTY}) may clear any cell;</li>
 *   <li>once a cell is empty again, any material may be placed on it.</li>
 * </ul>
 */
public final class BuildingGrid {
    public static final int SIZE = 24;

    private final int[][] cells = new int[SIZE][SIZE];

    public BuildingGrid() { }

    /** Builds a grid from existing data, validating shape and values. */
    public static BuildingGrid fromCells(int[][] source) {
        if (source == null || source.length != SIZE) {
            throw new IllegalArgumentException("Expected " + SIZE + " rows");
        }
        BuildingGrid grid = new BuildingGrid();
        for (int r = 0; r < SIZE; r++) {
            if (source[r] == null || source[r].length != SIZE) {
                throw new IllegalArgumentException("Row " + r + ": expected " + SIZE + " columns");
            }
            for (int c = 0; c < SIZE; c++) {
                int v = source[r][c];
                if (!Material.isValidValue(v)) {
                    throw new IllegalArgumentException("Cell (" + r + "," + c + "): invalid value " + v);
                }
                grid.cells[r][c] = v;
            }
        }
        return grid;
    }

    public int get(int row, int col) {
        checkCell(row, col);
        return cells[row][col];
    }

    /** Whether {@code tool} may be applied to the cell right now. */
    public boolean canPaint(int row, int col, int tool) {
        checkCell(row, col);
        checkTool(tool);
        return tool == Material.EMPTY || cells[row][col] == Material.EMPTY;
    }

    /**
     * Applies {@code tool} (a material id, or {@link Material#EMPTY} to erase).
     * @return true if the cell changed, false if the rules forbade it or it was already that value
     */
    public boolean paint(int row, int col, int tool) {
        if (!canPaint(row, col, tool) || cells[row][col] == tool) return false;
        cells[row][col] = tool;
        return true;
    }

    public boolean isEmpty() {
        for (int[] row : cells) {
            for (int v : row) {
                if (v != Material.EMPTY) return false;
            }
        }
        return true;
    }

    /** Deep copy of the cell values. */
    public int[][] toArray() {
        int[][] copy = new int[SIZE][];
        for (int r = 0; r < SIZE; r++) copy[r] = cells[r].clone();
        return copy;
    }

    private static void checkCell(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IndexOutOfBoundsException("Cell (" + row + "," + col + ") outside " + SIZE + "x" + SIZE);
        }
    }

    private static void checkTool(int tool) {
        if (!Material.isValidValue(tool)) {
            throw new IllegalArgumentException("Invalid tool/material id " + tool);
        }
    }
}