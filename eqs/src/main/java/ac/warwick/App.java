package ac.warwick;

import ac.warwick.core.Window;
import ac.warwick.core.Renderer;
import ac.warwick.core.ScreenManager;
import ac.warwick.core.HomeScreen;

import ac.warwick.wfc.WFC;
import ac.warwick.wfc.WFCViewer;

// TEST: RENDERABLES
import ac.warwick.core.Bitmap;
import ac.warwick.core.Label;
import ac.warwick.core.Button;

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

// unsure if safe
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_QUIT;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;


public final class App {

    public static final String TITLE  = "Earthquake Simulator";
    public static final int    WIDTH  = 1280;
    public static final int    HEIGHT = 720;

    private App() { }


    public static void main(String[] args) {
        // Java2D is only used to rasterise text/UI into off-screen images. Headless mode stops AWT
        // from initialising a native toolkit that could compete with SDL for the main thread (macOS).
        System.setProperty("java.awt.headless", "true");

        Window window = new Window(TITLE, WIDTH, HEIGHT);
        Renderer renderer = new Renderer(window);
        ScreenManager screens = new ScreenManager();

        screens.setScreen(new HomeScreen(renderer, screens));

        SDL_Event evt = new SDL_Event();
        boolean shouldRun = true;
        long last = System.nanoTime();

        try {
            while (shouldRun) {
                // 1. EVENTS: drain everything that happened since the last frame
                while (SDL_PollEvent(evt) != 0) {
                    if (evt.type == SDL_QUIT) {
                        shouldRun = false;            // closing the window works on every screen
                    } else {
                        screens.handleEvent(evt);     // everything else goes to the active screen
                    }
                }

                // 2. UPDATE: apply any pending screen switch, then run the screen's logic
                long now = System.nanoTime();
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                screens.update(dt);

                // 3. RENDER: draw (and update) everything in the renderer's queue
                renderer.render();
            }
        } finally {
            SDL_Quit();
        }
    }
}

