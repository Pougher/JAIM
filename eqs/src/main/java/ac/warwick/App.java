package ac.warwick;

import ac.warwick.core.Window;
import ac.warwick.core.Renderer;

import ac.warwick.wfc.WFC;

// TEST: RENDERABLES
import ac.warwick.core.Bitmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import io.github.libsdl4j.api.event.SDL_Event;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;

import static io.github.libsdl4j.api.error.SdlError.SDL_GetError;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.render.SdlRender.*;

class WFCViewer {
    private WFC wfc;
    private Bitmap bitmap;

    public WFCViewer(Renderer renderer) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("res/city-2.png"));
        } catch (IOException e) {
            System.out.println("Failed to load image");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        this.wfc = new WFC(
            image.getRGB(0, 0, width, height, null, 0, width),
            width,
            height,
            3
        );

        this.bitmap = new Bitmap(
            renderer,
            this.wfc.get_image_width(),
            this.wfc.get_image_height());

        this.run_wfc();
        renderer.enqueue_resource(this.bitmap);
    }

    public void run_wfc() {
        this.wfc.collapse();
        this.bitmap.from_int_array(this.wfc.get_pixels());
        this.bitmap.send_to_gpu();
    }
}

public class App {
    public static void main(String[] args) {
        Window window = new Window("EQS debug build", 1280, 720);
        Renderer renderer = new Renderer(window);
        WFCViewer viewer = new WFCViewer(renderer);

        // Start an event loop and react to events
        SDL_Event evt = new SDL_Event();
        boolean shouldRun = true;
        while (shouldRun) {
            // render to the screen
            renderer.render();

            while (SDL_PollEvent(evt) != 0) {
                switch (evt.type) {
                    case SDL_QUIT:
                        shouldRun = false;
                        break;
                    case SDL_KEYDOWN:
                        if (evt.key.keysym.sym == SDLK_SPACE) {
                            viewer.run_wfc();
                        }
                        break;
                    case SDL_WINDOWEVENT:
                        System.out.println("Window event " + evt.window.event);
                    default:
                        break;
                }
            }
        }

        SDL_Quit();
    }

}
