package ac.warwick.core;
import io.github.libsdl4j.api.event.SDL_Event;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.video.SDL_Window;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_EVERYTHING;
import static io.github.libsdl4j.api.error.SdlError.SDL_GetError;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_RESIZABLE;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.SDL_WINDOW_SHOWN;
import static io.github.libsdl4j.api.video.SdlVideo.SDL_CreateWindow;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;

// wrapper for SDL window. Only one SDL window may be created per application
public class Window {
    private int width;
    private int height;
    private String name;

    // the window is the actual window handle used by SDL. The renderer is used
    // to draw graphics to the screen
    private SDL_Window window;
    private SDL_Renderer renderer;

    private void open_window() {
        // open an SDL window and store the handle
        this.window = SDL_CreateWindow(
            this.name,
            SDL_WINDOWPOS_CENTERED,
            SDL_WINDOWPOS_CENTERED,
            this.width,
            this.height,
            SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE);

        if (window == null) {
            throw new IllegalStateException("Unable to create SDL window");
        }

    }

    public Window(String name, int width, int height) {
        this.width = width;
        this.height = height;

        this.name = name;

        // initialize SDL
        int result = SDL_Init(SDL_INIT_EVERYTHING);
        if (result != 0) {
            throw new IllegalStateException("Cannot initialize SDL library");
        }

        this.open_window();
    }

    public SDL_Window get_window() {
        return this.window;
    }
}
