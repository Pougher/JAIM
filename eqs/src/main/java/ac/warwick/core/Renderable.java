package ac.warwick.core;

import ac.warwick.core.Renderer;

public abstract class Renderable {
    public abstract void render(Renderer renderer);
    public abstract void update();
    public abstract void destroy();
}
