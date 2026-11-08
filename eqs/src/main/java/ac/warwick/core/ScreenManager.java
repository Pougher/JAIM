package ac.warwick;

import io.github.libsdl4j.api.event.SDL_Event;

public class ScreenManager {
    private Screen current;   // the screen that is active right now (null before the first frame)
    private Screen pending;   // a screen someone has asked for, not yet switched to

    public void setScreen(Screen next) {
        pending = next;       // only records the request; the switch happens in update()
    }

    public void handleEvent(SDL_Event evt) {
        if (current != null) current.handleEvent(evt);   // forward input to the active screen only
    }

    public void update(double dt) {
        if (pending != null) {                            // was a switch requested?
            if (current != null) current.onExit();        // 1. old screen cleans up (clears the renderer)
            current = pending;                            // 2. new screen becomes active
            pending = null;                               // 3. request fulfilled
            current.onEnter();                            // 4. new screen builds its visuals
        }
        if (current != null) current.update(dt);
    }
}