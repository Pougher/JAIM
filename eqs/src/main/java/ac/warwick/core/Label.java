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

public class Label extends Renderable {
    ByteBuffer pixels;
    SDL_Texture texture;
    SDL_Rect rect;

    public Label(Renderer renderer, String path, int x, int y) {
        this.rect = new SDL_Rect();

        BufferedImage image = null;

        try {
            image = ImageIO.read(new File(path));
        } catch (IOException e) {
            System.out.println("Failed to load image");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        rect.x = x;
        rect.y = y;
        rect.w = width;
        rect.h = height;

        this.pixels = image_to_buffer(image);
        this.texture = SDL_CreateTexture(
            renderer.get_renderer(),
            SDL_PIXELFORMAT_BGRA8888,
            SDL_TEXTUREACCESS_STATIC,
            width,
            height);

        // send the texture to the GPU
        int result = SDL_UpdateTexture(
            this.texture,
            null,
            Native.getDirectBufferPointer(this.pixels),
            width * 4);
    }

    public static ByteBuffer image_to_buffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                buffer.putInt(argb);
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
        SDL_DestroyTexture(this.texture);
    }
}
