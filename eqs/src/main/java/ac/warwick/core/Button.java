package ac.warwick.core;

import com.sun.jna.ptr.IntByReference;
import static io.github.libsdl4j.api.mouse.SdlMouse.SDL_GetMouseState;
//import static io.github.libsdl4j.api.render.SdlRender.SDL_SetTextureColorMod;

public class Button extends Label {
    //private enum State { NORMAL, HOVER, PRESSED }
    //private State state = State.NORMAL;
    private final Runnable onClick;
    private boolean wasDown = false;
    private boolean pressedHere = false;

    private final IntByReference mx = new IntByReference();
    private final IntByReference my = new IntByReference();

    public Button(Renderer renderer, String path, int x, int y, Runnable onClick) {
        super(renderer, path, x, y);
        this.onClick = onClick;
    }

    private boolean contains(int mx, int my) {
        return (mx >= rect.x && mx < rect.x + rect.w &&
                my >= rect.y && my < rect.y + rect.h);
    }

    @Override
    public void update() {
        int buttons = SDL_GetMouseState(mx, my);
        boolean isDown = (buttons & 1) != 0; // left mouse button
        boolean isInside = contains(mx.getValue(), my.getValue());

        if (isDown && !wasDown && isInside) {
            pressedHere = true;
        }

        if (wasDown && !isDown) {
            if (pressedHere && isInside) onClick.run();
            pressedHere = false;
        }

        /*if (isInside && pressedHere) {
            state = State.PRESSED;
        } else if (isInside) {
            state = State.HOVER;
        } else {
            state = State.NORMAL;
        }*/

        wasDown = isDown;
    }

    /*@Override
    public void render(Renderer renderer) {
        byte c;
        SDL_SetTextureColorMod(this.texture, (byte) 255, (byte) 255, (byte) 255); // default to white
        super.render(renderer);
        // TODO: pick a colour mod from `state`, then super.render(renderer)
    }*/
}