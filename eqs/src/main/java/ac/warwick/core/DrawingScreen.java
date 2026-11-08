package ac.warwick.core;

import ac.warwick.App;
import ac.warwick.core.Button;
import ac.warwick.core.GridCanvas;
import ac.warwick.core.Label;
import ac.warwick.core.Renderer;
import ac.warwick.core.UiGraphics;
import ac.warwick.drawing.BuildingGrid;

import ac.warwick.drawing.DrawingStore;
import ac.warwick.drawing.GridCsv; 
import ac.warwick.drawing.GridImages; 
import ac.warwick.drawing.Material;

import io.github.libsdl4j.api.event.SDL_Event;

import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEBUTTONDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_MOUSEMOTION;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Lets the user paint a 24x24 building out of six materials.
 *
 * <ul>
 *   <li>Left palette: one swatch + label per {@link Material}; clicking selects that material.</li>
 *   <li>Centre: the {@link GridCanvas}. A material can only be placed on an empty cell.</li>
 *   <li>Right: the pink eraser, which resets a cell to {@link Material#EMPTY}.</li>
 *   <li>Bottom: Submit, enabled only once at least one cell is painted. Saves CSV (+PNG)
 *       and moves on to {@link WFCScreen}.</li>
 * </ul>
 *
 * The screen is the only place that mutates a {@link BuildingGrid}, and it only ever does so
 * through {@link BuildingGrid#paint}, which enforces the placement rules.
 */
public final class DrawingScreen implements Screen {

    // ---- layout (window is App.WIDTH x App.HEIGHT = 1280 x 720) -----------------------------
    private static final int CELL_PX     = 20;
    private static final int CANVAS_PX   = BuildingGrid.SIZE * CELL_PX;          // 480
    private static final int CANVAS_X    = (App.WIDTH - CANVAS_PX) / 2;           // 400
    private static final int CANVAS_Y    = 56;

    private static final int   SWATCH_PX      = 80;
    private static final int[] SWATCH_COL_X   = { 72, 212 };
    private static final int[] SWATCH_ROW_Y   = { 96, 256, 416 };
    private static final int   LABEL_GAP      = 8;
    private static final int   LABEL_POINT    = 22;

    private static final int ERASER_X    = CANVAS_X + CANVAS_PX + 104;            // 984
    private static final int ERASER_Y    = CANVAS_Y;
    private static final int ERASER_POINT = 26;

    private static final int SUBMIT_W    = 400;
    private static final int SUBMIT_H    = 72;
    private static final int SUBMIT_X    = (App.WIDTH - SUBMIT_W) / 2;
    private static final int SUBMIT_Y    = CANVAS_Y + CANVAS_PX + 40;             // 576
    private static final int SUBMIT_POINT = 40;

    private static final int HINT_Y      = 14;
    private static final int STATUS_Y    = SUBMIT_Y + SUBMIT_H + 14;              // 662
    private static final int SMALL_POINT = 18;

    // ---- appearance -----------------------------------------------------------------------
    private static final Color ERASER_PINK     = new Color(0xECB2CB);
    private static final Color BORDER_IDLE     = new Color(0xB4B4B4);
    private static final Color BORDER_SELECTED = Color.WHITE;
    private static final int   BORDER_IDLE_PX     = 3;
    private static final int   BORDER_SELECTED_PX = 6;
    private static final int   GRID_LINE_ARGB     = 0xFFDDDDDD;   // subtle cell outlines on white

    // SDL mouse constants (same convention as Button, which masks SDL_GetMouseState with 1).
    private static final int LEFT_BUTTON = 1;   // SDL_BUTTON_LEFT
    private static final int LEFT_MASK   = 1;   // SDL_BUTTON_LMASK

    /** Painting continues while the left button is held and dragged across cells. */
    private static final boolean DRAG_PAINTING = true;

    private final Renderer renderer;
    private final ScreenManager screens;

    // model
    private final BuildingGrid grid = new BuildingGrid();
    private int tool = Material.STEEL.id();          // default material is colour 1 (Steel)

    // view
    private GridCanvas canvas;
    private final Map<Material, Button> swatches = new EnumMap<>(Material.class);
    private Button eraser;
    private Button submit;
    private Label status;
    private BufferedImage submitEnabledImage;
    private BufferedImage submitDisabledImage;

    // state
    private boolean submitEnabled;
    private boolean submitting;
    private String statusText = "";

    public DrawingScreen(Renderer renderer, ScreenManager screens) {
        this.renderer = renderer;
        this.screens = screens;
    }

    // ---- Screen ---------------------------------------------------------------------------

    @Override
    public void onEnter() {
        BufferedImage hint = UiGraphics.text("Select a material, then click the canvas to place it",
                SMALL_POINT, UiGraphics.MUTED_TEXT, UiGraphics.BLACK);
        renderer.enqueue_resource(new Label(renderer, hint, (App.WIDTH - hint.getWidth()) / 2, HINT_Y));

        canvas = new GridCanvas(renderer, CANVAS_X, CANVAS_Y,
                BuildingGrid.SIZE, BuildingGrid.SIZE, CELL_PX,
                Material.paletteArgb(), GRID_LINE_ARGB);
        renderer.enqueue_resource(canvas);

        // Palette: Steel/Timber, Glass/Concrete, Brick/Stone – two columns, three rows.
        for (Material material : Material.values()) {
            int index = material.ordinal();
            int x = SWATCH_COL_X[index % 2];
            int y = SWATCH_ROW_Y[index / 2];
            Button swatch = new Button(renderer, swatchImage(material.color(), false), x, y,
                    () -> selectTool(material.id()));
            swatches.put(material, swatch);
            renderer.enqueue_resource(swatch);
            renderer.enqueue_resource(labelUnder(material.displayName(), LABEL_POINT, x, y));
        }

        eraser = new Button(renderer, swatchImage(ERASER_PINK, false), ERASER_X, ERASER_Y,
                () -> selectTool(Material.EMPTY));
        renderer.enqueue_resource(eraser);
        renderer.enqueue_resource(labelUnder("Eraser", ERASER_POINT, ERASER_X, ERASER_Y));

        submitEnabledImage = UiGraphics.button("Submit", SUBMIT_W, SUBMIT_H, SUBMIT_POINT,
                UiGraphics.PRIMARY_BLUE, UiGraphics.WHITE);
        submitDisabledImage = UiGraphics.button("Submit", SUBMIT_W, SUBMIT_H, SUBMIT_POINT,
                UiGraphics.DISABLED_GREY, UiGraphics.MUTED_TEXT);
        submit = new Button(renderer, submitDisabledImage, SUBMIT_X, SUBMIT_Y, this::submit);
        submit.setEnabled(false);
        submitEnabled = false;
        renderer.enqueue_resource(submit);

        status = new Label(renderer, UiGraphics.solid(1, 1, UiGraphics.BLACK), 0, STATUS_Y);
        renderer.enqueue_resource(status);

        submitting = false;
        selectTool(tool);   // applies the selection highlight and the status line
    }

    @Override
    public void onExit() {
        renderer.clear_resources();   // destroys every texture created above
        swatches.clear();
        canvas = null;
        eraser = null;
        submit = null;
        status = null;
    }

    @Override
    public void handleEvent(SDL_Event evt) {
        if (canvas == null) return;

        if (evt.type == SDL_MOUSEBUTTONDOWN) {
            if (evt.button.button == LEFT_BUTTON) {
                paintAt(evt.button.x, evt.button.y);
            }
        } else if (DRAG_PAINTING && evt.type == SDL_MOUSEMOTION) {
            if ((evt.motion.state & LEFT_MASK) != 0) {
                paintAt(evt.motion.x, evt.motion.y);
            }
        }
    }

    @Override
    public void update(double dt) { }

    // ---- interaction ----------------------------------------------------------------------

    private void selectTool(int toolId) {
        tool = toolId;
        for (Map.Entry<Material, Button> entry : swatches.entrySet()) {
            Material material = entry.getKey();
            entry.getValue().setImage(swatchImage(material.color(), material.id() == toolId));
        }
        eraser.setImage(swatchImage(ERASER_PINK, toolId == Material.EMPTY));

        setStatus(toolId == Material.EMPTY
                ? "Eraser selected: click a cell to clear it"
                : "Material: " + Material.fromId(toolId).displayName());
    }

    /** Applies the current tool to the cell under (px, py), if any, respecting the placement rules. */
    private void paintAt(int px, int py) {
        int row = canvas.rowAt(py);
        int col = canvas.colAt(px);
        if (row < 0 || col < 0) return;                   // click was outside the canvas

        int before = grid.get(row, col);
        if (grid.paint(row, col, tool)) {
            canvas.set(row, col, grid.get(row, col));
            refreshSubmitState();
        } else if (tool != Material.EMPTY && before != Material.EMPTY) {
            setStatus("That cell already contains " + Material.nameOf(before) + ": erase it first");
        }
    }

    private void refreshSubmitState() {
        boolean enabled = !grid.isEmpty();
        if (enabled != submitEnabled) {
            submitEnabled = enabled;
            submit.setEnabled(enabled);
            submit.setImage(enabled ? submitEnabledImage : submitDisabledImage);
        }
    }

        private void submit() {
        if (submitting) return;
        if (grid.isEmpty()) {
            setStatus("Draw at least one cell before submitting");
            return;
        }
        submitting = true;
        try {
            DrawingStore.Saved saved = DrawingStore.save(grid, GridCsv.defaultDirectory());
            System.out.println("[DrawingScreen] Saved " + saved.csv + " and " + saved.latestPng);
            screens.setScreen(new WFCScreen(renderer, screens));
        } catch (IOException | RuntimeException e) {
            submitting = false;
            System.err.println("[DrawingScreen] Failed to save drawing: " + e);
            setStatus("Could not save drawing: " + e.getMessage());
        }
    }

    // ---- helpers --------------------------------------------------------------------------

    private static BufferedImage swatchImage(Color fill, boolean selected) {
        return UiGraphics.swatch(SWATCH_PX, fill,
                selected ? BORDER_SELECTED : BORDER_IDLE,
                selected ? BORDER_SELECTED_PX : BORDER_IDLE_PX);
    }

    /** A centred white caption directly beneath a swatch whose top-left corner is (x, y). */
    private Label labelUnder(String text, int pointSize, int x, int y) {
        BufferedImage img = UiGraphics.text(text, pointSize, UiGraphics.WHITE, UiGraphics.BLACK);
        return new Label(renderer, img, x + (SWATCH_PX - img.getWidth()) / 2, y + SWATCH_PX + LABEL_GAP);
    }

    private void setStatus(String text) {
        if (status == null || text.equals(statusText)) return;   // avoid re-uploading an identical texture
        statusText = text;
        BufferedImage img = UiGraphics.text(text, SMALL_POINT, UiGraphics.MUTED_TEXT, UiGraphics.BLACK);
        status.setImage(img);
        status.setPosition((App.WIDTH - img.getWidth()) / 2, STATUS_Y);
    }
}