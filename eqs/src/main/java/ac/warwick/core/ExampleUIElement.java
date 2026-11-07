package ac.warwick.core;

import ac.warwick.core.Renderable;
import ac.warwick.core.Renderer;

public class ExampleUIElement extends Renderable {
    // if you need the mouse as input, you might need a reference to the Window
    // class and use the underlying SDL window (just pass that in when you
    // instantiate this object)
    public ExampleUIElement() {
        System.out.println("I am an example element");
    }

    public void render(Renderer renderer) {
        // if you need to access the underlying SDL_Renderer, just do
        // renderer.get_renderer()
        // RENDERING CODE HERE
    }

    // update can be used to update the state of the object every frame - not
    // for rendering
    public void update() {
    }
}
