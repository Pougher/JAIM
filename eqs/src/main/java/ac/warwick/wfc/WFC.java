package ac.warwick.wfc;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;

import ac.warwick.core.Renderer;
import ac.warwick.core.Bitmap;

class Pattern {
    public int[] pixels;

    public Pattern(int[] data) {
        this.pixels = data;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pattern)) return false;

        Pattern patt = (Pattern) other;
        if (Arrays.equals(pixels, patt.pixels)) {
            return true;
        }

        return false;
    }

    public boolean matches_right(Pattern other, int n) {
        for (int y = 0; y < n; y++) {
            for (int x = 1; x < n; x++) {
                if (this.pixels[y * n + x] != other.pixels[y * n + x - 1]) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean matches_left(Pattern other, int n) {
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n - 1; x++) {
                if (this.pixels[y * n + x] != other.pixels[y * n + x + 1]) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean matches_top(Pattern other, int n) {
        for (int y = 0; y < n - 1; y++) {
            for (int x = 0; x < n; x++) {
                if (this.pixels[y * n + x] != other.pixels[(y + 1) * n + x]) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean matches_bottom(Pattern other, int n) {
        for (int y = 1; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (this.pixels[y * n + x] != other.pixels[(y - 1) * n + x]) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pixels);
    }
}

enum Direction {
    TOP, BOTTOM, LEFT, RIGHT
}

public class WFC {
    // the input image featureset
    private int[] input_image;
    private int[] output_image;

    private int image_width;
    private int image_height;
    private int pattern_size;

    private ArrayList<Pattern> patterns = new ArrayList<>();
    private HashMap<Pattern, Integer>
        patterncounts = new HashMap<Pattern, Integer>();

    // adjacency matrix, determining what patterns can be located next to
    // eachother
    private Map<Pattern, EnumMap<Direction, Set<Pattern>>> adjacencies =
        new HashMap<>();

    public WFC(
        int[] input,
        int image_width,
        int image_height,
        int pattern_size) {
        this.pattern_size = pattern_size;
        this.image_width = image_width;
        this.image_height = image_height;
        this.input_image = input;
    }

    public void collapse() {
        this.find_patterns();
        this.gen_adjacency_map();
    }

    public void dump_patterns(Renderer rend) {
        List<Pattern> patternlist = new ArrayList<>(this.patterns);

        System.out.println("Dumping patterns " + patternlist.size());
        for (int i = 0; i < this.patterns.size(); i++) {
            Bitmap bmp = new Bitmap(
                rend, this.pattern_size, this.pattern_size);
            bmp.set_position(i * this.pattern_size * 10, 0);
            bmp.from_int_array(patternlist.get(i).pixels);
            bmp.send_to_gpu();
            rend.enqueue_resource(bmp);

            // debug print
            System.out.println(
                "pattern: " + i + " Occurances: " +
                this.patterncounts.get(patternlist.get(i)));
        }
    }

    private void find_patterns() {
        // lovely O(n^4) algorithm
        for (int x = 0; x <= this.image_width - this.pattern_size; x++) {
            for (int y = 0; y <= this.image_height - this.pattern_size; y++) {
                int pattern_data[] =
                    new int[this.pattern_size * this.pattern_size];
                for (int i = 0; i < this.pattern_size; i++) {
                    for (int j = 0; j < this.pattern_size; j++) {
                        pattern_data[i + j * this.pattern_size] =
                            this.input_image[
                            i + x + (j + y) * this.image_width];
                    }
                }
                this.patterns.add(new Pattern(pattern_data));
            }
        }

        // count occurances of each pattern
        for (Pattern pattern : this.patterns) {
            this.patterncounts.put(
                pattern,
                this.patterncounts.getOrDefault(pattern, 0) + 1);
        }
        // now, remove all of the duplicate patterns
        this.deduplicate();
    }

    // returns an enummap containing all of the adjacencies of a given pattern
    EnumMap<Direction, Set<Pattern>> find_adjacencies(Pattern p) {
        Set<Pattern> set_top = new HashSet<Pattern>();
        Set<Pattern> set_bottom = new HashSet<Pattern>();
        Set<Pattern> set_left = new HashSet<Pattern>();
        Set<Pattern> set_right = new HashSet<Pattern>();

        for (Pattern pattern : this.patterns) {
            if (p.matches_right(pattern, this.pattern_size))
                set_right.add(pattern);
            if (p.matches_left(pattern, this.pattern_size))
                set_left.add(pattern);
            if (p.matches_top(pattern, this.pattern_size))
                set_top.add(pattern);
            if (p.matches_bottom(pattern, this.pattern_size))
                set_bottom.add(pattern);
        }

        EnumMap<Direction, Set<Pattern>> patternmap =
            new EnumMap<Direction, Set<Pattern>>(Direction.class);
        patternmap.put(Direction.RIGHT, set_right);
        patternmap.put(Direction.LEFT, set_left);
        patternmap.put(Direction.TOP, set_top);
        patternmap.put(Direction.BOTTOM, set_bottom);

        return patternmap;
    }

    public void gen_adjacency_map() {
        for (Pattern pattern : this.patterns) {
            EnumMap<Direction, Set<Pattern>> possible_adjacencies =
                this.find_adjacencies(pattern);
            this.adjacencies.put(pattern, possible_adjacencies);
        }
    }

    // removes duplicate patterns from the patternset
    private void deduplicate() {
        Set<Pattern> set = new HashSet<>(this.patterns);
        this.patterns.clear();
        this.patterns.addAll(set);
    }
}
