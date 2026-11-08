package ac.warwick.core;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;

import java.io.IOException;
import io.github.libsdl4j.api.rect.SDL_Rect;
import com.sun.jna.Native;

import com.sun.jna.Native;

import io.github.libsdl4j.api.render.SDL_Texture;
import static io.github.libsdl4j.api.render.SdlRender.*;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.*;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.*;
import io.github.libsdl4j.api.rect.SDL_Rect;

import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_BGRA8888;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.SDL_TEXTUREACCESS_STATIC;
import static io.github.libsdl4j.api.render.SdlRender.*;

/**
 * A static image drawn at a fixed position. Can be created from a file on disk or from an
 * in-memory {@link BufferedImage}, and its image can be swapped later with {@link #setImage}.
 */
public class Label extends Renderable {
    protected final Renderer renderer;
    ByteBuffer pixels;
    SDL_Texture texture;
    SDL_Rect rect;

    public Label(Renderer renderer, String path, int x, int y) {
        this(renderer, loadImage(path), x, y);
    }

    public Label(Renderer renderer, BufferedImage image, int x, int y) {
        this.renderer = renderer;
        this.rect = new SDL_Rect();
        this.rect.x = x;
        this.rect.y = y;
        upload(image);
    }

    private static BufferedImage loadImage(String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            if (image == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image: " + path);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load image: " + path, e);
        }
    }

    /** Replaces the displayed image. Reuses the GPU texture when the size is unchanged. */
    public void setImage(BufferedImage image) {
        if (texture != null && image.getWidth() == rect.w && image.getHeight() == rect.h) {
            pixels = image_to_buffer(image);
            SDL_UpdateTexture(texture, null, Native.getDirectBufferPointer(pixels), rect.w * 4);
        } else {
            if (texture != null) SDL_DestroyTexture(texture);
            upload(image);
        }
    }

    public void setPosition(int x, int y) {
        rect.x = x;
        rect.y = y;
    }

    public int getX()      { return rect.x; }
    public int getY()      { return rect.y; }
    public int getWidth()  { return rect.w; }
    public int getHeight() { return rect.h; }

    private void upload(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        rect.w = width;
        rect.h = height;

        pixels = image_to_buffer(image);
        texture = SDL_CreateTexture(
            renderer.get_renderer(),
            SDL_PIXELFORMAT_BGRA8888,
            SDL_TEXTUREACCESS_STATIC,
            width,
            height);
        if (texture == null) {
            throw new IllegalStateException("SDL_CreateTexture failed (" + width + "x" + height + ")");
        }
        SDL_UpdateTexture(texture, null, Native.getDirectBufferPointer(pixels), width * 4);
    }

    public static ByteBuffer image_to_buffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);   // big-endian by default
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer.putInt(image.getRGB(x, y));   // ARGB big-endian == BGRA8888 little-endian
            }
        }
        buffer.flip();
        return buffer;
    }

    public void render(Renderer renderer) {
        SDL_RenderCopy(renderer.get_renderer(), this.texture, null, this.rect);
    }

    public void update() {}

    public void destroy() {
        if (texture != null) {
            SDL_DestroyTexture(texture);
            texture = null;
        }
    }
}