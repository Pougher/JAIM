package ac.warwick.core;

import java.nio.ByteBuffer;

import io.github.libsdl4j.api.render.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.*;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.*;
import io.github.libsdl4j.api.rect.SDL_Rect;
import com.sun.jna.Native;

import ac.warwick.core.Renderable;
import ac.warwick.core.Renderer;

public class Bitmap extends Renderable {
    private int x;
    private int y;
    private int width;
    private int height;

    // pixels are scaled up by 8x
    private int default_scaling = 8;

    // the actual pixel buffer
    public ByteBuffer pixels;
    private SDL_Texture texture;

    public Bitmap(Renderer renderer, int width, int height) {
        this.x = 0;
        this.y = 0;
        this.width = width;
        this.height = height;

        // allocate the bytebuffer (fuck you garbage collector)
        this.pixels = ByteBuffer.allocateDirect(this.width * this.height * 4);

        // create the GPU texture
        this.texture = SDL_CreateTexture(
            renderer.get_renderer(),
            SDL_PIXELFORMAT_RGBA8888,
            SDL_TEXTUREACCESS_STATIC,
            this.width,
            this.height);
    }

    // after you have modified the pixels inside of the bytebuffer, you should
    // upload the changes to the GPU to see them
    public void send_to_gpu() {
        int result = SDL_UpdateTexture(
            this.texture,
            null,
            Native.getDirectBufferPointer(this.pixels),
            width * 4);
    }

    public void render(Renderer renderer) {
        SDL_Rect dest = new SDL_Rect();
        dest.x = this.x;
        dest.y = this.y;
        dest.w = this.width * this.default_scaling;
        dest.h = this.height * this.default_scaling;
        SDL_RenderCopy(renderer.get_renderer(), this.texture, null, dest);
    }

    public void update() {
        return;
    }

    public int get_width() {
        return this.width;
    }

    public int get_height() {
        return this.height;
    }
}
