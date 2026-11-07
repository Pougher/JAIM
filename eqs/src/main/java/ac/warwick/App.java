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

public class App {
    public static void main(String[] args) {
        Window window = new Window("EQS debug build", 1280, 720);
        Renderer renderer = new Renderer(window);

        Bitmap bm = new Bitmap(renderer, 64, 64);

        // setup some pixel data?
        for (int i = 0; i < bm.get_width() * bm.get_height() * 4; i+=4) {
            bm.pixels.put((byte) 255);
            bm.pixels.put((byte) 255);
            bm.pixels.put((byte) 0);
            bm.pixels.put((byte) 0);
        }

        bm.pixels.flip();
        bm.send_to_gpu();

        //renderer.enqueue_resource(bm);

        try {
            BufferedImage image = ImageIO.read(new File("res/city.png"));
            int width = image.getWidth();
            int height = image.getHeight();
            WFC test = new WFC(
                image.getRGB(0, 0, width, height, null, 0, width),
                width,
                height,
                4
            );
            test.collapse();
            test.dump_patterns(renderer);
        } catch (IOException e) {
            System.out.println("Failed to load image");
        }

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
                            System.out.println("SPACE pressed");
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
