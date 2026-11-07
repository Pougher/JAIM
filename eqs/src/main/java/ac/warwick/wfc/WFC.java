package ac.warwick.wfc;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.BitSet;
import java.util.Random;
import java.util.ArrayDeque;

import ac.warwick.core.Renderer;
import ac.warwick.core.Bitmap;

enum Direction {
    TOP, BOTTOM, LEFT, RIGHT
}

// abstraction over patterns (a set of pixels, which occurs in the image)
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

    // routines to check if the left/right/top/bottom edge of the pattern
    // matches the edge of this pattern
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

// perfect cell
class Cell {
    public BitSet possible_patterns;
    public Pattern collapsed_pattern;

    public Cell(int total_patterns) {
        this.possible_patterns = new BitSet(total_patterns);

        // set all of the patterns to 1s, as the bitset is in a superposition of
        // states and can be anything
        this.possible_patterns.set(0, total_patterns);
    }

    public BitSet get_possibilities() {
        return this.possible_patterns;
    }

    public boolean is_collapsed() {
        return this.possible_patterns.cardinality() == 1;
    }

    public boolean is_contradiction() {
        return this.possible_patterns.isEmpty();
    }
}

public class WFC {
    // random generator which will be used for this instance of WFC
    private Random random;

    // the input image featureset
    private int[] input_image;
    private int[] output_image;

    private int image_width;
    private int image_height;
    private int pattern_size;

    private int output_width = 32;
    private int output_height = 32;

    private ArrayList<Pattern> patterns = new ArrayList<>();
    private HashMap<Pattern, Integer>
        patterncounts = new HashMap<Pattern, Integer>();

    // adjacency matrix, determining what patterns can be located next to
    // eachother
    private Map<Pattern, EnumMap<Direction, Set<Pattern>>> adjacencies =
        new HashMap<>();

    // cells which will be collapsed into a set state
    private Cell[][] wavegrid;

    // maps a given pattern to an index
    private Map<Pattern, Integer> pattern_indices = new HashMap<>();

    public WFC(
        int[] input,
        int image_width,
        int image_height,
        int pattern_size) {
        this.pattern_size = pattern_size;
        this.image_width = image_width;
        this.image_height = image_height;
        this.input_image = input;

        this.random = new Random();
    }

    public boolean collapse() {
        this.find_patterns();
        this.gen_adjacency_map();
        this.build_pattern_indices();

        final int MAX_RETRIES = 20;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            this.init_wave();

            while (true) {
                int pos[] = this.find_lowest_entropy();
                if (pos == null) {
                    System.out.println("Finished collapsing!");
                    return true;
                }

                this.collapse_cell(pos[0], pos[1]);

                if (!propogate(pos[0], pos[1])) {
                    break;
                }
            }

            System.out.println("CONTRADICTED!");
        }

        return false;
    }

    public int get_image_width() {
        return this.output_width + this.pattern_size - 1;
    }

    public int get_image_height() {
        return this.output_height + this.pattern_size - 1;
    }

    public int[] get_pixels() {
        int image_width = this.output_width + this.pattern_size - 1;
        int image_height = this.output_height + this.pattern_size - 1;

        int[] pix = new int[image_width * image_height];
        for (int x = 0; x < this.output_width; x++) {
            for (int y = 0; y < this.output_height; y++) {
                Cell cell = this.wavegrid[x][y];

                if (!cell.is_collapsed()) {
                    System.out.println("Grid has an uncollapsed cell?");
                    return null;
                }

                int pattern_index = cell.possible_patterns.nextSetBit(0);
                Pattern pattern = this.patterns.get(pattern_index);

                // copy the pattern index into the resulting image
                for (int py = 0; py < this.pattern_size; py++) {
                    for (int px = 0; px < this.pattern_size; px++) {
                        int pixel = pattern.pixels[py * this.pattern_size + px];
                        pix[x + px + (py + y) * image_width] = pixel;
                    }
                }
            }
        }

        return pix;
    }

    public void init_wave() {
        this.wavegrid = new Cell[this.output_width][this.output_height];

        for (int x = 0; x < this.output_width; x++) {
            for (int y = 0; y < this.output_height; y++) {
                this.wavegrid[x][y] = new Cell(this.patterns.size());
            }
        }

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
        }
    }

    private void find_patterns() {
        // clear out old patterns (if there are any)
        this.patterns.clear();
        this.patterncounts.clear();

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
    private EnumMap<Direction, Set<Pattern>> find_adjacencies(Pattern p) {
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
        this.adjacencies.clear();

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

    // chooses a weighted random index from the bitset
    private int wave_random(BitSet possibilities) {
        double total_weight = 0;

        for (int i = possibilities.nextSetBit(0);
            i >= 0;
            i = possibilities.nextSetBit(i + 1)) {
            total_weight += this.patterncounts.get(this.patterns.get(i));
        }

        double choice = this.random.nextDouble() * total_weight;
        for (int i = possibilities.nextSetBit(0);
            i >= 0;
            i = possibilities.nextSetBit(i + 1)) {
            choice -= this.patterncounts.get(this.patterns.get(i));

            if (choice < 0) return i;
        }

        System.out.println("NO PATTERNS AVAILABLE!");
        return -1;
    }

    private double compute_entropy(BitSet possibilities) {
        double total_weight = 0;
        double weighted_log_sum = 0;

        for (int i = possibilities.nextSetBit(0);
            i >= 0;
            i = possibilities.nextSetBit(i + 1)) {
            double weight = patterncounts.get(patterns.get(i));
            total_weight += weight;
            weighted_log_sum += weight * Math.log(weight);
        }

        if (total_weight == 0) return Double.POSITIVE_INFINITY;

        return Math.log(total_weight) - weighted_log_sum / total_weight;
    }

    private int[] find_lowest_entropy() {
        double lowest_entropy = Double.POSITIVE_INFINITY;
        int[] result = null;

        for (int x = 0; x < this.output_width; x++) {
            for (int y = 0; y < this.output_height; y++) {
                Cell cell = this.wavegrid[x][y];

                if (cell.is_collapsed() || cell.is_contradiction()) {
                    continue;
                }

                double entropy =
                    this.compute_entropy(cell.get_possibilities());

                // a little bit of random noise in order to break ties
                entropy += random.nextDouble() * 0.0001;

                if (entropy < lowest_entropy) {
                    lowest_entropy = entropy;
                    result = new int[] { x, y };
                }
            }
        }

        return result;
    }

    private void collapse_cell(int x, int y) {
        Cell cell = this.wavegrid[x][y];
        int chosen = this.wave_random(cell.get_possibilities());

        cell.possible_patterns.clear();
        cell.possible_patterns.set(chosen);
    }

    // pattern indices are required for ensuring collapsed patterns are
    // consistent with their pattern values
    private void build_pattern_indices() {
        this.pattern_indices.clear();

        for (int i = 0; i < patterns.size(); i++) {
            this.pattern_indices.put(this.patterns.get(i), i);
        }
    }

    private boolean propogate(int start_x, int start_y) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { start_x, start_y });

        while (!queue.isEmpty()) {
            int[] pos = queue.removeFirst();
            int x = pos[0];
            int y = pos[1];

            Cell current = this.wavegrid[x][y];

            for (Direction dir : Direction.values()) {
                int dx = x;
                int dy = y;

                switch (dir) {
                    case TOP: { dy--; break; }
                    case BOTTOM: { dy++; break; }
                    case LEFT: { dx--; break; }
                    case RIGHT: { dx++; break; }
                }

                // we don't care about neighbouring cells which are outside of
                // the grid
                if (dx < 0 || dy < 0 ||
                    dx >= this.output_width || dy >= this.output_height) {
                    continue;
                }

                Cell neighbour = this.wavegrid[dx][dy];
                BitSet allowed = new BitSet(this.patterns.size());

                for (int i = current.possible_patterns.nextSetBit(0);
                    i >= 0;
                    i = current.possible_patterns.nextSetBit(i + 1)) {
                    Pattern p = this.patterns.get(i);

                    Set<Pattern> compatible_patterns = this.adjacencies
                        .get(p)
                        .get(dir);

                    for (Pattern candidate : compatible_patterns) {
                        Integer index = this.pattern_indices.get(candidate);

                        if (index != null) {
                            allowed.set(index);
                        }
                    }
                }

                // now, remove impossible patterns from the neighbour cell
                BitSet previous = (BitSet) neighbour.possible_patterns.clone();
                neighbour.possible_patterns.and(allowed);

                if (!previous.equals(neighbour.possible_patterns)) {
                    if (neighbour.is_contradiction()) {
                        return false;
                    }

                    queue.addLast(new int[] { dx, dy });
                }
            }
        }

        return true;
    }
}
