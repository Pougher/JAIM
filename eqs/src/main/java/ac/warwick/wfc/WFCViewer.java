package ac.warwick.wfc;

import ac.warwick.wfc.Building;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import java.util.Objects;
import org.json.simple.JSONObject;

import ac.warwick.core.Bitmap;
import ac.warwick.core.Renderer;
import netscape.javascript.JSException;

/**
 * Displays a single WFC output as a {@link Bitmap} and extracts buildings from it.
 *
 * <p>The sample image is supplied by the caller (normally the user's latest drawing); nothing is
 * loaded from a fixed path here. Must be used from the main (SDL) thread.</p>
 */
public class WFCViewer {
    public static final int PATTERN_SIZE = WfcImageGenerator.DEFAULT_PATTERN_SIZE;

    private final WFC wfc;
    private final Bitmap bitmap;
    private final int output_width;
    private final int output_height;
    private int[] pixels;   // cached result of the most recent successful run

    /**
     * @param sample image the WFC learns from; must be at least {@value #PATTERN_SIZE} pixels square
     * @throws IllegalStateException if the first generation fails outright
     */
    public WFCViewer(Renderer renderer, BufferedImage sample) {
        Objects.requireNonNull(renderer, "renderer");
        this.wfc = WfcImageGenerator.newWfc(sample, PATTERN_SIZE);
        this.output_width = this.wfc.get_image_width();
        this.output_height = this.wfc.get_image_height();
        this.bitmap = new Bitmap(renderer, this.output_width, this.output_height);

        if (!this.run_wfc()) {
            throw new IllegalStateException("WFC could not generate an initial image from the sample");
        }
        renderer.enqueue_resource(this.bitmap);
    }

    /**
     * Generates a new image and uploads it to the GPU.
     * @return true on success; false if WFC gave up, in which case the previous image is kept
     */
    public boolean run_wfc() {
        if (!this.wfc.collapse()) {
            System.err.println("[WFCViewer] WFC exhausted its retries; keeping previous image");
            return false;
        }
        int[] result = this.wfc.get_pixels();
        if (result == null) {                        // previously this would NPE inside from_int_array
            System.err.println("[WFCViewer] WFC output incomplete; keeping previous image");
            return false;
        }
        this.pixels = result;
        this.bitmap.from_int_array(this.pixels);
        this.bitmap.send_to_gpu();
        return true;
    }

    /** Gets a single pixel from the cached output (previously re-rendered the whole image per call). */
    public int get_pixel(int x, int y) {
        return this.pixels[x + y * this.output_width];
    }

    /**
     * A pixel that is not part of any building. Black/transparent was the original convention
     * (city-3.png); white is the empty colour of the in-app drawing canvas.
     */
    public boolean blank(int col) {
        return col == 0xff000000 || col == 0x00 || col == 0xffffffff;
    }

    // starting from the ground, we will attempt to extend our building to the
    // highest Y value possible. Then we will extend it across until we
    // encounter a different X pixel. A blank pixel is NOT a building
    public Building find_building_group(int start_x) {
        int height = 0;
        int width = 0;
        ArrayList<Integer> materials = new ArrayList<>();

        // first extrude up
        while (height < this.output_height
            && !this.blank(this.get_pixel(start_x, this.output_height - height - 1))) {
            height++;
        }

        for (int x = start_x; x < this.output_width; x++) {
            boolean valid_column = true;

            // Check whether the entire column belongs to the building
            for (int y = this.output_height - 1; y >= this.output_height - height; y--) {
                if (this.blank(this.get_pixel(x, y))) {
                    valid_column = false;
                    break;
                }
            }

            // if we aren't at the top, a taller building could be next to this
            // one, so check the pixel above
            if (valid_column && height < this.output_height
                && !this.blank(this.get_pixel(x, this.output_height - height - 1))) {
                valid_column = false;   // we are in another building
            }

            if (!valid_column) break;

            // Store the materials in this column
            for (int y = this.output_height - 1; y >= this.output_height - height; y--) {
                materials.add(this.get_pixel(x, y));
            }
            width++;
        }

        return new Building(
            start_x,
            this.output_height - height,
            width,
            height,
            materials.stream().mapToInt(Integer::intValue).toArray());
    }

    public Building[] find_buildings() {
        ArrayList<Building> buildings = new ArrayList<>();
        int x_pos = 0;

        while (x_pos < this.output_width) {
            if (this.blank(this.get_pixel(x_pos, this.output_height - 1))) {
                x_pos++;
                continue;
            }

            Building b = this.find_building_group(x_pos);
            buildings.add(b);
            x_pos += Math.max(1, b.width);   // guarantees progress even for a degenerate building
        }

        return buildings.toArray(new Building[0]);
    }

    /** Writes all detected buildings to {@code buildings.json}. */
    @SuppressWarnings("unchecked")
    public void serialize() {
        Building[] buildings = this.find_buildings();
        JSONObject output = new JSONObject();
        for (int i = 0; i < buildings.length; i++) {
            output.put(String.valueOf(i), buildings[i].serialize());
        }

        try (FileWriter file = new FileWriter("buildings.json")) {
            output.writeJSONString(file);
        } catch (IOException e) {
            System.err.println("[WFCViewer] Failed to write buildings.json: " + e.getMessage());
        }
    }
}