package ac.warwick.core;

import com.sun.jna.ptr.IntByReference;
import static io.github.libsdl4j.api.mouse.SdlMouse.SDL_GetMouseState;
//import static io.github.libsdl4j.api.render.SdlRender.SDL_SetTextureColorMod;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * A {@link Label} that fires a callback on a completed left click (press and release both inside).
 * Mouse state is polled each frame in {@link #update()}, so no event plumbing is required.
 */
public class Button extends Label {
    private final Runnable onClick;
    private boolean enabled = true;
    private boolean wasDown = false;
    private boolean pressedHere = false;

    private final IntByReference mx = new IntByReference();
    private final IntByReference my = new IntByReference();

    public Button(Renderer renderer, String path, int x, int y, Runnable onClick) {
        super(renderer, path, x, y);
        this.onClick = Objects.requireNonNull(onClick, "onClick");
    }

    public Button(Renderer renderer, BufferedImage image, int x, int y, Runnable onClick) {
        super(renderer, image, x, y);
        this.onClick = Objects.requireNonNull(onClick, "onClick");
    }

    /** A disabled button still renders but ignores clicks. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) pressedHere = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean contains(int px, int py) {
        return (px >= rect.x && px < rect.x + rect.w &&
                py >= rect.y && py < rect.y + rect.h);
    }

    @Override
    public void update() {
        int buttons = SDL_GetMouseState(mx, my);
        boolean isDown = (buttons & 1) != 0;   // left mouse button
        boolean isInside = contains(mx.getValue(), my.getValue());

        if (isDown && !wasDown && isInside && enabled) {
            pressedHere = true;
        }

        if (wasDown && !isDown) {
            if (pressedHere && isInside && enabled) onClick.run();
            pressedHere = false;
        }

        wasDown = isDown;
    }
}