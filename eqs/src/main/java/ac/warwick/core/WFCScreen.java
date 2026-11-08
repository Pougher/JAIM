package ac.warwick.core;

import ac.warwick.core.Renderer;
import ac.warwick.wfc.WFCSerializer;
import ac.warwick.wfc.WFCViewer;
import ac.warwick.App;
import ac.warwick.core.Label;
import ac.warwick.core.UiGraphics;
import ac.warwick.drawing.BuildingGrid;
import ac.warwick.drawing.DrawingStore;
import ac.warwick.wfc.WfcImageGenerator;
import ac.warwick.drawing.GridCsv;
import ac.warwick.drawing.GridImages;

import io.github.libsdl4j.api.event.SDL_Event;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_H;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_SPACE;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_1;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_2;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_3;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.SDLK_4;

// unclear if necessary
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYDOWN;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

/**
 * Shows the most recently submitted drawing, then on SPACE generates four WFC variants of it
 * and displays them in a 2x2 grid. H returns to the home screen.
 *
 * <p>Generation runs on a background thread so the window stays responsive; results are turned
 * into textures on the main thread in {@link #update}, which is the only thread allowed to touch
 * SDL. A second SPACE while a batch is running is ignored.</p>
 */
public final class WFCScreen implements Screen {
    // ---- layout ---------------------------------------------------------------------------
    private static final int PREVIEW_CELL_PX = 10;
    private static final int CONTENT_Y       = 80;
    private static final int TILE_GAP        = 16;
    private static final int TILE_PX         = 272;   // 2*272+16 = 560 tall
    private static final int GRID_PX         = 2 * TILE_PX + TILE_GAP;
    private static final int GRID_X          = (App.WIDTH - GRID_PX) / 2;
    private static final int CAPTION_Y       = App.HEIGHT - 56;
    private static final int CAPTION_POINT   = 20;
    private static final int GRID_LINE_RGB   = 0xDDDDDD;

    // ---- generation -----------------------------------------------------------------------
    private static final int VARIANTS            = 4;
    private static final int ATTEMPTS_PER_VARIANT = 20;   // WFC can hit contradictions; retry a few times
    private static final String CONTROLS = "SPACE: generate 4 variants     1-4: select H: home";

    private final Renderer renderer;
    private final ScreenManager screens;
    private final Path drawingsDir;
    private final WfcImageGenerator generator = new WfcImageGenerator();

    private BufferedImage input;                 // validated latest.png, null if loading failed
    private ExecutorService worker;
    private Future<List<Variant>> pending; // non-null while a batch is in flight
    private Label caption;
    private List<Variant> variants;
    private record Variant(BufferedImage raw, BufferedImage tile) {}

    /** Uses the most recently submitted drawing in the default drawings directory. */
    public WFCScreen(Renderer renderer, ScreenManager screens) {
        this(renderer, screens, GridCsv.defaultDirectory());
    }

    public WFCScreen(Renderer renderer, ScreenManager screens, Path drawingsDir) {
        this.renderer = renderer;
        this.screens = screens;
        this.drawingsDir = drawingsDir;
    }

    // ---- Screen ---------------------------------------------------------------------------

    @Override
    public void onEnter() {
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "wfc-worker");
            t.setDaemon(true);
            return t;
        });
        pending = null;
        input = null;

        String text;
        try {
            DrawingStore.Loaded loaded = DrawingStore.loadLatest(drawingsDir);
            input = loaded.image;
            BufferedImage preview = UiGraphics.fitNearest(loaded.image, GRID_PX);   // crisp integer upscale
            renderer.enqueue_resource(new Label(renderer, preview,
                    (App.WIDTH - preview.getWidth()) / 2, CONTENT_Y));
            text = "Loaded " + loaded.pngPath.getFileName()
                    + (loaded.pngRepaired ? " (rebuilt from CSV)" : "") + "     |     " + CONTROLS;
        } catch (IOException | RuntimeException e) {
            System.err.println("[WFCScreen] Could not load latest drawing from '" + drawingsDir + "': " + e.getMessage());
            text = "No valid drawing could be loaded. Press H and submit a drawing first.";
        }
        caption = new Label(renderer, captionImage(text), 0, CAPTION_Y);
        centreCaption();
        renderer.enqueue_resource(caption);
    }

    @Override
    public void onExit() {
        if (pending != null) pending.cancel(true);
        pending = null;
        if (worker != null) worker.shutdownNow();
        worker = null;
        caption = null;
        variants = null;
        renderer.clear_resources();
    }

    @Override
    public void handleEvent(SDL_Event evt) {
        if (evt.type != SDL_KEYDOWN) return;

        int key = evt.key.keysym.sym;
        if (key == SDLK_SPACE) {
            startGeneration();
        } else if (key == SDLK_H) {
            screens.setScreen(new HomeScreen(renderer, screens));
        } else if (key >= SDLK_1 && key <= SDLK_4) {
            selectVariant(key - SDLK_1);
        }
    }

    public void selectVariant(int index) {
        if (variants == null || pending != null || variants.get(index) == null) {
            return;
        }

        System.out.println("Selected variant " + (index + 1));
        BufferedImage image = this.variants.get(index).raw();

        File outputfile = new File("image.jpg");
        try { ImageIO.write(image, "png", outputfile); } catch (Exception e) {}

        // turn the buffered image into int[]
        int width = image.getWidth();
        int height = image.getHeight();
        System.out.println("W" + width + "H" + height);

        int[] pixels = image.getRGB(
            0,
            0,
            width,
            height,
            null,
            0,
            width
        );

        System.out.println("SERIALIZING");

        // save it to JSON
        WFCSerializer serializer = new WFCSerializer(
            pixels, width, height);
        serializer.serialize("buildings.json");
    }

    private List<Variant> generateBatch(BufferedImage sample) {
        List<Variant> out = new ArrayList<>(VARIANTS);
        for (int v = 0; v < VARIANTS; v++) {
            BufferedImage result = null;
            for (int attempt = 0; attempt < ATTEMPTS_PER_VARIANT && result == null; attempt++) {
                if (Thread.currentThread().isInterrupted()) return out;
                try {
                    result = generator.generate(sample, v);
                } catch (RuntimeException e) {
                    System.err.println("[WFCScreen] variant " + v + " attempt " + (attempt + 1) + " failed: " + e.getMessage());
                }
            }

            out.add(
                result == null ? null :
                new Variant(
                    result,
                    UiGraphics.fitNearest(result, TILE_PX)));
        }
        return out;
    }

    @Override
    public void update(double dt) {
        if (pending == null || !pending.isDone()) return;

        List<Variant> results;
        try {
            results = pending.get();
        } catch (CancellationException | InterruptedException e) {
            Thread.currentThread().interrupt();
            pending = null;
            return;
        } catch (ExecutionException e) {
            pending = null;
            System.err.println("[WFCScreen] Generation failed: " + e.getCause());
            setCaption("Generation failed: " + e.getCause().getMessage() + "     |     " + CONTROLS);
            return;
        }
        pending = null;
        showVariants(results);
    }

    // ---- generation -----------------------------------------------------------------------

    private void startGeneration() {
        if (pending != null) return;                      // a batch is already running
        if (input == null) {
            setCaption("Nothing to generate from. Press H and submit a drawing first.");
            return;
        }
        final BufferedImage sample = input;
        setCaption("Generating " + VARIANTS + " variants...");
        pending = worker.submit(() -> generateBatch(sample));
    }

    /** Runs on the worker thread. Never touches SDL. Null entries mark variants that failed. */
    /**private List<BufferedImage> generateBatch(BufferedImage sample) {
        List<BufferedImage> out = new ArrayList<>(VARIANTS);
        for (int v = 0; v < VARIANTS; v++) {
            BufferedImage result = null;
            for (int attempt = 0; attempt < ATTEMPTS_PER_VARIANT && result == null; attempt++) {
                if (Thread.currentThread().isInterrupted()) return out;
                try {
                    result = generator.generate(sample, v);
                } catch (RuntimeException e) {
                    System.err.println("[WFCScreen] variant " + v + " attempt " + (attempt + 1) + " failed: " + e.getMessage());
                }
            }
            out.add(result == null ? null : UiGraphics.fitNearest(result, TILE_PX));
        }
        return out;
    }*/

    /** Runs on the main thread: swaps the preview for the 2x2 grid of variants. */
    private void showVariants(List<Variant> variants) {
        this.variants = variants;
        renderer.clear_resources();                        // also destroys the old caption's texture
        int failed = 0;

        for (int i = 0; i < VARIANTS; i++) {
            int col = i % 2;
            int row = i / 2;
            int cellX = GRID_X + col * (TILE_PX + TILE_GAP);
            int cellY = CONTENT_Y + row * (TILE_PX + TILE_GAP);

            Variant v = i < variants.size() ? variants.get(i) : null;
            BufferedImage tile = (v == null) ? null : v.tile();

            if (tile == null) {
                failed++;
                tile = UiGraphics.button("Variant " + (i + 1) + " failed", TILE_PX, TILE_PX,
                        22, UiGraphics.DISABLED_GREY, UiGraphics.WHITE);
            }
            renderer.enqueue_resource(new Label(renderer, tile,
                    cellX + (TILE_PX - tile.getWidth()) / 2,
                    cellY + (TILE_PX - tile.getHeight()) / 2));
        }

        String text = failed == 0
                ? "Generated " + VARIANTS + " variants     |     " + CONTROLS
                : failed + " of " + VARIANTS + " variants failed (contradiction)     |     " + CONTROLS;
        caption = new Label(renderer, captionImage(text), 0, CAPTION_Y);
        centreCaption();
        renderer.enqueue_resource(caption);
    }

    // ---- caption helpers ------------------------------------------------------------------

    private static BufferedImage captionImage(String text) {
        return UiGraphics.text(text, CAPTION_POINT, UiGraphics.MUTED_TEXT, UiGraphics.BLACK);
    }

    private void setCaption(String text) {
        if (caption == null) return;
        caption.setImage(captionImage(text));
        centreCaption();
    }

    private void centreCaption() {
        caption.setPosition((App.WIDTH - caption.getWidth()) / 2, CAPTION_Y);
    }
}