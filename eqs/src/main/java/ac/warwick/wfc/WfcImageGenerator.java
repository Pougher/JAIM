package ac.warwick.wfc;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * The single point of contact between the application and {@link WFC}.
 *
 * <p>Converts a sample {@link BufferedImage} into the pixel array WFC expects, runs the collapse,
 * and converts the result back into an image. Pure Java (no SDL), so it is safe to call from a
 * background thread. A fresh {@link WFC} is built per call: it is cheap for small samples and
 * guarantees every variant uses its own random state.</p>
 */
public final class WfcImageGenerator {
    public static final int DEFAULT_PATTERN_SIZE = 3;

    private final int patternSize;

    public WfcImageGenerator() {
        this(DEFAULT_PATTERN_SIZE);
    }

    public WfcImageGenerator(int patternSize) {
        if (patternSize < 2) throw new IllegalArgumentException("patternSize must be at least 2");
        this.patternSize = patternSize;
    }

    /**
     * Generates one variant learned from {@code sample}.
     *
     * @param sample  the input image (the user's latest drawing)
     * @param variant index within the batch; currently informational only, since {@link WFC}
     *                seeds itself. Kept so a seed can be threaded through later without touching callers.
     * @throws IllegalStateException if WFC exhausted its retries without a contradiction-free result
     */
    public BufferedImage generate(BufferedImage sample, int variant) {
        WFC wfc = newWfc(sample, patternSize);
        if (!wfc.collapse()) {
            throw new IllegalStateException(
                "WFC could not find a contradiction-free output (variant " + variant + ")");
        }

        return toImage(wfc);
    }

    /** Builds a {@link WFC} from an image, validating that the image can actually yield patterns. */
    static WFC newWfc(BufferedImage sample, int patternSize) {
        Objects.requireNonNull(sample, "sample");
        int w = sample.getWidth();
        int h = sample.getHeight();
        if (w < patternSize || h < patternSize) {
            throw new IllegalArgumentException("Sample is " + w + "x" + h
                    + " but must be at least " + patternSize + "x" + patternSize);
        }
        int[] argb = sample.getRGB(0, 0, w, h, null, 0, w);
        return new WFC(argb, w, h, patternSize);
    }

    /** Converts the collapsed WFC output into an ARGB image. */
    static BufferedImage toImage(WFC wfc) {
        int[] pixels = wfc.get_pixels();
        if (pixels == null) {
            throw new IllegalStateException("WFC output still contains uncollapsed cells");
        }
        int w = wfc.get_image_width();
        int h = wfc.get_image_height();
        if (pixels.length != w * h) {
            throw new IllegalStateException("WFC returned " + pixels.length + " pixels for a " + w + "x" + h + " image");
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, pixels, 0, w);
        return img;
    }
}