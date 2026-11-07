package ac.warwick;

import ac.warwick.core.Window;
import ac.warwick.core.Renderer;

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
