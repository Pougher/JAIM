package ac.warwick;

import ac.warwick.core.Renderer;
import ac.warwick.wfc.WFCViewer;
import ac.warwick.core.Label;
import ac.warwick.core.UiGraphics;
import ac.warwick.drawing.BuildingGrid;
import ac.warwick.drawing.GridCsv;
import ac.warwick.drawing.GridImages;

import io.github.libsdl4j.api.event.SDL_Event;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_H;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;

// unclear if necessary
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYDOWN;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Shows the drawing that was just submitted (loaded back from disk and validated),
 * then hands over to the existing WFC implementation on SPACE. H returns home.
 */
public final class WFCScreen implements Screen {
    private static final int PREVIEW_CELL_PX = 20;
    private static final int PREVIEW_Y       = 80;
    private static final int CAPTION_Y       = App.HEIGHT - 56;
    private static final int CAPTION_POINT   = 20;
    private static final int GRID_LINE_RGB   = 0xDDDDDD;
    private static final String CONTROLS     = "SPACE: generate with WFC     H: home";

    private final Renderer renderer;
    private final ScreenManager screens;
    private final Path drawingPath;

    private WFCViewer viewer;          // created lazily on the first SPACE so each visit starts fresh

    /** Uses the most recently submitted drawing ({@code latest.csv}). */
    public WFCScreen(Renderer renderer, ScreenManager screens) {
        this(renderer, screens, GridCsv.latest(GridCsv.defaultDirectory()));
    }

    public WFCScreen(Renderer renderer, ScreenManager screens, Path drawingPath) {
        this.renderer = renderer;
        this.screens = screens;
        this.drawingPath = drawingPath;
    }

    @Override
    public void onEnter() {
        viewer = null;
        String caption;
        try {
            BuildingGrid grid = GridCsv.load(drawingPath);           // validated: 24x24, values 0..6
            BufferedImage preview = GridImages.render(grid, PREVIEW_CELL_PX, GRID_LINE_RGB);
            renderer.enqueue_resource(new Label(renderer, preview,
                    (App.WIDTH - preview.getWidth()) / 2, PREVIEW_Y));
            caption = "Loaded " + drawingPath.getFileName() + "     |     " + CONTROLS;
        } catch (IOException | RuntimeException e) {
            System.err.println("[WFCScreen] Could not load drawing '" + drawingPath + "': " + e.getMessage());
            caption = "No valid drawing could be loaded     |     " + CONTROLS;
        }
        enqueueCaption(caption);
    }

    @Override
    public void onExit() {
        renderer.clear_resources();   // removes the preview and everything the viewer put on screen
        viewer = null;
    }

    @Override
    public void handleEvent(SDL_Event evt) {
        if (evt.type != SDL_KEYDOWN) return;

        if (evt.key.keysym.sym == SDLK_SPACE) {
            generate();
        } else if (evt.key.keysym.sym == SDLK_H) {
            screens.setScreen(new HomeScreen(renderer, screens));
        }
    }

    @Override
    public void update(double dt) { }

    private void generate() {
        if (viewer == null) {
            renderer.clear_resources();          // hand the whole screen to the WFC output
            viewer = new WFCViewer(renderer);
            viewer.run_wfc();
            enqueueCaption(CONTROLS);            // queued last so it draws on top
        } else {
            viewer.run_wfc();                    // regenerate, exactly as before
        }
    }

    private void enqueueCaption(String text) {
        BufferedImage img = UiGraphics.text(text, CAPTION_POINT, UiGraphics.MUTED_TEXT, UiGraphics.BLACK);
        renderer.enqueue_resource(new Label(renderer, img, (App.WIDTH - img.getWidth()) / 2, CAPTION_Y));
    }
}