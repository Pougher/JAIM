package ac.warwick;

import ac.warwick.core.Label;
import ac.warwick.core.Renderer;
import ac.warwick.core.Button;
import ac.warwick.core.UiGraphics;

import io.github.libsdl4j.api.event.SDL_Event;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_W;

import java.awt.image.BufferedImage;

/**
 * Title screen: black background, "Earthquake Simulator" heading and a single
 * "Start Simulation" button that leads to the {@link DrawingScreen}.
 * Deliberately has no keyboard shortcuts.
 */
public final class HomeScreen implements Screen {
    private static final int TITLE_POINT_SIZE   = 72;
    private static final int TITLE_CENTRE_Y     = 250;

    private static final int BUTTON_WIDTH       = 560;
    private static final int BUTTON_HEIGHT      = 124;
    private static final int BUTTON_Y           = 366;
    private static final int BUTTON_POINT_SIZE  = 44;

    private final Renderer renderer;
    private final ScreenManager screens;

    public HomeScreen(Renderer renderer, ScreenManager screens) {
        this.renderer = renderer;
        this.screens = screens;
    }

    @Override
    public void onEnter() {
        BufferedImage title = UiGraphics.text("Earthquake Simulator", TITLE_POINT_SIZE,
                UiGraphics.WHITE, UiGraphics.BLACK);
        renderer.enqueue_resource(new Label(renderer, title,
                (App.WIDTH - title.getWidth()) / 2,
                TITLE_CENTRE_Y - title.getHeight() / 2));

        BufferedImage start = UiGraphics.button("Start Simulation", BUTTON_WIDTH, BUTTON_HEIGHT,
                BUTTON_POINT_SIZE, UiGraphics.PRIMARY_BLUE, UiGraphics.WHITE);
        renderer.enqueue_resource(new Button(renderer, start,
                (App.WIDTH - BUTTON_WIDTH) / 2, BUTTON_Y,
                () -> screens.setScreen(new DrawingScreen(renderer, screens))));
    }

    @Override
    public void onExit() {
        renderer.clear_resources();
    }

    @Override
    public void handleEvent(SDL_Event evt) {
        // Intentionally empty: navigation from the home screen is by the button only.
    }

    @Override
    public void update(double dt) { }
}