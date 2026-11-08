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
            System.err.println(
                "[WFCViewer] WFC exhausted its retries; keeping previous image");
            return false;
        }
        int[] result = this.wfc.get_pixels();
        if (result == null) { // previously this would NPE inside from_int_array
            System.err.println("[WFCViewer] WFC output incomplete; keeping previous image");
            return false;
        }
        this.pixels = result;
        this.bitmap.from_int_array(this.pixels);
        this.bitmap.send_to_gpu();
        return true;
    }
    /**
 * Exports the generated city's buildings to JSON.
 */
    public void serialize(String path) {
        if (this.pixels == null) {
            System.err.println("[WFCViewer] No WFC output available to serialize.");
            return;
        }

        WFCSerializer serializer = new WFCSerializer(
            this.pixels,
            this.output_width,
            this.output_height
        );

        serializer.serialize(path);
    }
}