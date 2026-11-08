package ac.warwick;

import ac.warwick.core.Window;
import ac.warwick.core.Renderer;

import ac.warwick.wfc.WFC;
import ac.warwick.wfc.WFCViewer;

// TEST: RENDERABLES
import ac.warwick.core.Bitmap;
import ac.warwick.core.Label;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import io.github.libsdl4j.api.event.SDL_Event;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;

import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_KP_ENTER;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_S;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;

public class App {
    public static void main(String[] args) {
        Window window = new Window("EQS debug build", 1280, 720);
        Renderer renderer = new Renderer(window);
        WFCViewer viewer = new WFCViewer(renderer);

        // this is how you use the Label class btw
        renderer.enqueue_resource(
            new Label(renderer, "res/city-3.png", 700, 0));

        // Start an event loop and react to events
        SDL_Event evt = new SDL_Event();
        boolean shouldRun = true;
        while (shouldRun) {
            while (SDL_PollEvent(evt) != 0) {
                switch (evt.type) {
                    case SDL_QUIT:
                        shouldRun = false;
                        break;
                    case SDL_KEYDOWN:
                        if (evt.key.keysym.sym == SDLK_SPACE) {
                            viewer.run_wfc();
                        }
                        if (evt.key.keysym.sym == SDLK_S) {
                            viewer.serialize();
                        }
                        break;
                    case SDL_WINDOWEVENT:
                        break;
                    default:
                        break;
                }
            }

            // render to the screen
            renderer.render();
        }

        SDL_Quit();
    }

}
