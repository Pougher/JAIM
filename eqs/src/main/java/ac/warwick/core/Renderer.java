package ac.warwick.core;

import java.util.HashMap;

import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.*;
import io.github.libsdl4j.api.render.SDL_Renderer;

import ac.warwick.core.Window;
import ac.warwick.core.Renderable;

// renderer wrapper for the SDL renderer
public class Renderer {
    private SDL_Renderer renderer;

    // items to be rendered on the next frame
    private HashMap<Long, Renderable> renderables = new HashMap<>();
    private long renderable_id = 0;

    public Renderer(Window window) {
        this.renderer = SDL_CreateRenderer(
            window.get_window(), -1, SDL_RENDERER_ACCELERATED);

        if (this.renderer == null) {
            throw new IllegalStateException("Unable to create SDL renderer");
        }

        // for now, set our default rendering colour to black
        SDL_SetRenderDrawColor(renderer, (byte)255, (byte)0, (byte)0, (byte)255);
        SDL_RenderClear(renderer);
        SDL_RenderPresent(renderer);
    }

    // displays all of the renderables to the screen
    public void render() {
        for (Renderable renderable : this.renderables.values()) {
            renderable.render(this);
        }
    }

    // adds a renderable resource to the queue of things to be rendered on the
    // next frame. This queue IS persistent.
    public long enqueue_resource(Renderable renderable) {
        this.renderable_id++;
        this.renderables.put(this.renderable_id, renderable);

        return this.renderable_id;
    }

    // removes a resource from the rendering pipeline
    public void dequeue_resource(long id) {
        this.renderables.remove(id);
    }

    public SDL_Renderer get_renderer() {
        return this.renderer;
    }
}
