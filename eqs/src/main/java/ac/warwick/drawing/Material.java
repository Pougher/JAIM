package ac.warwick.drawing;

import java.awt.Color;

/**
 * The six building materials a cell can hold. Ids 1–6 are the values stored in the grid and
 * the CSV; {@link #EMPTY} (0) means "nothing drawn yet".
 */
public enum Material {
    STEEL   (1, "Steel",    0x808080),
    TIMBER  (2, "Timber",   0xA67A5B),
    GLASS   (3, "Glass",    0xADD8E6),
    CONCRETE(4, "Concrete", 0xE8E2B8),
    BRICK   (5, "Brick",    0x7B242A),
    STONE   (6, "Stone",    0x7D8EB8);

    public static final int EMPTY     = 0;
    public static final int EMPTY_RGB = 0x000000;
    public static final int MAX_ID    = values().length;

    private final int id;
    private final String displayName;
    private final int rgb;

    Material(int id, String displayName, int rgb) {
        this.id = id;
        this.displayName = displayName;
        this.rgb = rgb;
    }

    public int    id()          { return id; }
    public String displayName() { return displayName; }
    public int    rgb()         { return rgb; }
    public Color  color()       { return new Color(rgb); }

    public static Material fromId(int id) {
        for (Material m : values()) {
            if (m.id == id) return m;
        }
        throw new IllegalArgumentException("No material with id " + id);
    }

    public static boolean isValidValue(int value) {
        return value >= EMPTY && value <= MAX_ID;
    }

    /** Human-readable name of a cell value, including "Empty" for 0. */
    public static String nameOf(int value) {
        return value == EMPTY ? "Empty" : fromId(value).displayName;
    }

    public static int rgbOf(int value) {
        return value == EMPTY ? EMPTY_RGB : fromId(value).rgb;
    }

    /** Opaque ARGB colour for every cell value, indexed by value (0 = white). */
    public static int[] paletteArgb() {
        int[] palette = new int[MAX_ID + 1];
        palette[EMPTY] = 0xFF000000 | EMPTY_RGB;
        for (Material m : values()) palette[m.id] = 0xFF000000 | m.rgb;
        return palette;
    }
}