package ac.warwick;

import io.github.libsdl4j.api.event.SDL_Event;

public interface Screen {
    void onEnter();                  // runs once when this screen becomes active: create visuals here
    void onExit();                   // runs once when leaving: clean up here
    void handleEvent(SDL_Event evt); // keyboard/mouse input, only while this screen is active
    void update(double dt);          // per-frame logic; dt = seconds since last frame
}