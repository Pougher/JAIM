package ac.warwick.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Off-screen rasterisation of UI elements (text, buttons, swatches) into opaque
 * {@link BufferedImage}s, ready to be wrapped in a {@link Label} or {@link Button}.
 *
 * Text is drawn with Java2D. If the JVM has no usable fonts (e.g. a minimal container image),
 * the methods degrade to text-free placeholder shapes instead of throwing, so the UI keeps working.
 */
public final class UiGraphics {
    public static final Color BLACK         = Color.BLACK;
    public static final Color WHITE         = Color.WHITE;
    public static final Color PRIMARY_BLUE  = new Color(0x5B9BD5);
    public static final Color DISABLED_GREY = new Color(0x5A5A5A);
    public static final Color MUTED_TEXT    = new Color(0xB4B4B4);

    private static final String FONT_FAMILY   = Font.SANS_SERIF;
    private static final int    TEXT_PAD_X    = 6;
    private static final int    TEXT_PAD_Y    = 4;
    private static volatile boolean fontsUsable = true;

    static {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
    }

    private UiGraphics() { }

    /** Text on a solid background, sized to fit the string. */
    public static BufferedImage text(String text, int pointSize, Color fg, Color bg) {
        Font font = new Font(FONT_FAMILY, Font.PLAIN, pointSize);
        Dimension size = measure(text, font);
        if (size == null) return placeholder(text, pointSize, fg, bg);

        BufferedImage img = opaque(size.width, size.height, bg);
        if (!drawCentred(img, text, font, fg)) return placeholder(text, pointSize, fg, bg);
        return img;
    }

    /** A filled rectangle of the given size with centred text (plain rectangle if fonts fail). */
    public static BufferedImage button(String text, int width, int height, int pointSize, Color bg, Color fg) {
        BufferedImage img = opaque(width, height, bg);
        drawCentred(img, text, new Font(FONT_FAMILY, Font.PLAIN, pointSize), fg);
        return img;
    }

    /** A square colour swatch with a border. */
    public static BufferedImage swatch(int size, Color fill, Color border, int borderPx) {
        BufferedImage img = opaque(size, size, border);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(fill);
            g.fillRect(borderPx, borderPx, size - 2 * borderPx, size - 2 * borderPx);
        } finally {
            g.dispose();
        }
        return img;
    }

    public static BufferedImage solid(int width, int height, Color colour) {
        return opaque(width, height, colour);
    }

    // ---- internals ------------------------------------------------------------------------

    private static BufferedImage opaque(int width, int height, Color bg) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(bg);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
        } finally {
            g.dispose();
        }
        return img;
    }

    private static Dimension measure(String text, Font font) {
        if (!fontsUsable) return null;
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            FontMetrics fm = g.getFontMetrics(font);
            return new Dimension(fm.stringWidth(text) + 2 * TEXT_PAD_X, fm.getHeight() + 2 * TEXT_PAD_Y);
        } catch (Throwable t) {          // font subsystem failures surface as java.lang.Error
            fontsUnavailable(t);
            return null;
        } finally {
            g.dispose();
        }
    }

    private static boolean drawCentred(BufferedImage img, String text, Font font, Color fg) {
        if (!fontsUsable) return false;
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font);
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            int x = (img.getWidth() - fm.stringWidth(text)) / 2;
            int y = (img.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
            return true;
        } catch (Throwable t) {
            fontsUnavailable(t);
            return false;
        } finally {
            g.dispose();
        }
    }

    /** Text-free stand-in with roughly the footprint the text would have had. */
    private static BufferedImage placeholder(String text, int pointSize, Color fg, Color bg) {
        int w = (int) Math.round(text.length() * pointSize * 0.55) + 2 * TEXT_PAD_X;
        int h = (int) Math.round(pointSize * 1.4) + 2 * TEXT_PAD_Y;
        BufferedImage img = opaque(w, h, bg);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(fg);
            g.drawRect(1, 1, img.getWidth() - 3, img.getHeight() - 3);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void fontsUnavailable(Throwable cause) {
        if (fontsUsable) {
            fontsUsable = false;
            System.err.println("[UiGraphics] Text rendering unavailable; falling back to placeholders: " + cause);
        }
    }
}