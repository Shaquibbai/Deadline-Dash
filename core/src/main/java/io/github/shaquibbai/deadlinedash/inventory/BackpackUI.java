package io.github.shaquibbai.deadlinedash.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/**
 * UI panel for displaying the player's Backpack inventory in a clean GRID format.
 * Supports unlimited items arranged in grid boxes with vertical scrolling.
 */
public class BackpackUI {
    private final Backpack backpack;
    private boolean open = false;

    // Scrolling state
    private float scrollY = 0f;
    private final GlyphLayout layout = new GlyphLayout();

    // Virtual Viewport Dimensions: 1280 x 720
    // Centered Panel dimensions
    private static final float PANEL_W = 660f;
    private static final float PANEL_H = 510f;
    private static final float PANEL_X = (1280f - PANEL_W) / 2f; // 310f (Dead-Center)
    private static final float PANEL_Y = (720f - PANEL_H) / 2f;  // 105f (Dead-Center)

    private static final float HEADER_H = 52f;
    private static final float GRID_TOP_Y = PANEL_Y + PANEL_H - HEADER_H - 16f; // Top Y of first row
    private static final float GRID_HEIGHT = 415f; // Visible grid area height

    // Grid Column & Slot dimensions (4 Columns per row)
    private static final int COLS = 4;
    private static final float SLOT_W = 132f;
    private static final float SLOT_H = 118f;
    private static final float GAP_X = 16f;
    private static final float GAP_Y = 14f;
    private static final float ROW_STRIDE = SLOT_H + GAP_Y; // 132f

    // Grid X Start position inside panel (centered columns)
    private static final float GRID_X = PANEL_X + (PANEL_W - (COLS * SLOT_W + (COLS - 1) * GAP_X)) / 2f; // 322f

    // Close button (Top-Right of header)
    private final Rectangle closeBtn = new Rectangle(PANEL_X + PANEL_W - 44f, PANEL_Y + PANEL_H - 44f, 36f, 36f);

    public BackpackUI(Backpack backpack) {
        this.backpack = backpack;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (open) {
            scrollY = 0f;
        }
    }

    public void toggle() {
        setOpen(!open);
    }

    /**
     * Scroll wheel handler called from input listener or screen scroll update.
     */
    public void scroll(float amountY) {
        if (!open) return;
        scrollY += amountY * 30f;
    }

    public void update(float delta, Vector3 mousePos, boolean justClicked) {
        if (!open) return;

        // Close button click check
        if (justClicked && mousePos != null && closeBtn.contains(mousePos.x, mousePos.y)) {
            open = false;
        }
    }

    /**
     * Renders the centered Grid-based Backpack UI overlay.
     */
    public void render(SpriteBatch batch, BitmapFont titleFont, BitmapFont itemFont, Texture whitePixel, Vector3 mousePos) {
        if (!open) return;

        // 1. Dim Backdrop Overlay
        batch.setColor(0f, 0f, 0f, 0.60f);
        batch.draw(whitePixel, 0, 0, 1280, 720);

        // 2. Main Centered Inventory Panel Container
        batch.setColor(0.10f, 0.12f, 0.16f, 0.97f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // Header Background Bar
        batch.setColor(0.15f, 0.19f, 0.26f, 1.0f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, HEADER_H);

        // Outer Accent Border
        batch.setColor(0.35f, 0.52f, 0.78f, 0.90f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 2, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, 2, PANEL_H);
        batch.draw(whitePixel, PANEL_X + PANEL_W - 2, PANEL_Y, 2, PANEL_H);

        // Header Separator Line
        batch.setColor(0.25f, 0.35f, 0.50f, 0.85f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, 2);

        // Header Title Text ("BACKPACK")
        titleFont.setColor(new Color(1.0f, 0.85f, 0.3f, 1.0f));
        layout.setText(titleFont, "BACKPACK");
        titleFont.draw(batch, "BACKPACK", PANEL_X + (PANEL_W - layout.width) / 2f, PANEL_Y + PANEL_H - (HEADER_H - layout.height) / 2f);

        // Close Button [X]
        boolean closeHover = mousePos != null && closeBtn.contains(mousePos.x, mousePos.y);
        batch.setColor(closeHover ? new Color(0.85f, 0.25f, 0.25f, 1f) : new Color(0.22f, 0.27f, 0.35f, 1f));
        batch.draw(whitePixel, closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height);

        itemFont.setColor(Color.WHITE);
        layout.setText(itemFont, "X");
        itemFont.draw(batch, "X", closeBtn.x + (closeBtn.width - layout.width) / 2f, closeBtn.y + (closeBtn.height + layout.height) / 2f);

        // 3. Render Grid of Slots
        List<Backpack.Entry> entries = backpack.getEntries();
        int totalItems = entries.size();

        // Always render at least 12 slots (3 rows of 4 columns)
        int minTotalSlots = 12;
        int totalSlots = Math.max(minTotalSlots, ((totalItems + COLS - 1) / COLS) * COLS);
        int totalRows = (totalSlots + COLS - 1) / COLS;

        float totalGridContentH = totalRows * ROW_STRIDE - GAP_Y;
        float maxScroll = Math.max(0f, totalGridContentH - (GRID_HEIGHT - 30f));
        scrollY = MathUtils.clamp(scrollY, 0f, maxScroll);

        float gridBottomY = PANEL_Y + 15f;

        for (int slotIdx = 0; slotIdx < totalSlots; slotIdx++) {
            int col = slotIdx % COLS;
            int row = slotIdx / COLS;

            float slotX = GRID_X + col * (SLOT_W + GAP_X);
            float slotY = GRID_TOP_Y - (row * ROW_STRIDE) + scrollY - SLOT_H;

            // Clip slots outside visible grid bounds
            if (slotY + SLOT_H < gridBottomY || slotY > GRID_TOP_Y) {
                continue;
            }

            Backpack.Entry entry = (slotIdx < totalItems) ? entries.get(slotIdx) : null;

            // Render Slot Box Container
            boolean slotHover = entry != null && mousePos != null && mousePos.x >= slotX && mousePos.x <= slotX + SLOT_W && mousePos.y >= slotY && mousePos.y <= slotY + SLOT_H;

            if (entry != null) {
                batch.setColor(slotHover ? new Color(0.20f, 0.26f, 0.36f, 0.95f) : new Color(0.14f, 0.17f, 0.23f, 0.95f));
            } else {
                batch.setColor(0.11f, 0.13f, 0.18f, 0.70f); // Clean empty slot
            }
            batch.draw(whitePixel, slotX, slotY, SLOT_W, SLOT_H);

            // Slot Box Border
            batch.setColor(slotHover ? new Color(0.95f, 0.78f, 0.25f, 0.95f) : (entry != null ? new Color(0.28f, 0.38f, 0.52f, 0.85f) : new Color(0.20f, 0.24f, 0.32f, 0.50f)));
            batch.draw(whitePixel, slotX, slotY, SLOT_W, 2);
            batch.draw(whitePixel, slotX, slotY + SLOT_H - 2, SLOT_W, 2);
            batch.draw(whitePixel, slotX, slotY, 2, SLOT_H);
            batch.draw(whitePixel, slotX + SLOT_W - 2, slotY, 2, SLOT_H);

            // Render Occupied Slot Contents (Item Name, Icon, Quantity)
            if (entry != null) {
                Item item = entry.getItem();
                String itemName = item.getName();

                // A. Item Name (Top of slot box)
                itemFont.setColor(Color.WHITE);
                layout.setText(itemFont, itemName);

                // Truncate name if wider than slot
                float nameDrawX = slotX + (SLOT_W - layout.width) / 2f;
                itemFont.draw(batch, itemName, Math.max(slotX + 4f, nameDrawX), slotY + SLOT_H - 10f);

                // B. Item Icon (Center of slot box)
                renderItemIcon(batch, whitePixel, itemName, slotX + SLOT_W / 2f, slotY + SLOT_H / 2f - 2f);

                // C. Quantity (e.g., "x10", "x5", "x3", "x500" - Bottom Right)
                String qtyStr = "x" + entry.getQuantity();
                itemFont.setColor(new Color(1.0f, 0.85f, 0.3f, 1.0f));
                layout.setText(itemFont, qtyStr);
                itemFont.draw(batch, qtyStr, slotX + SLOT_W - layout.width - 8f, slotY + 20f);
            }
        }

        // 4. Render Scrollbar if grid rows exceed visible container
        if (maxScroll > 0f) {
            float scrollbarW = 6f;
            float scrollbarX = PANEL_X + PANEL_W - 14f;
            float scrollbarTrackH = GRID_HEIGHT - 30f;
            float scrollbarTrackY = gridBottomY + 10f;

            // Scrollbar Track
            batch.setColor(0.18f, 0.22f, 0.28f, 0.8f);
            batch.draw(whitePixel, scrollbarX, scrollbarTrackY, scrollbarW, scrollbarTrackH);

            // Thumb
            float thumbH = Math.max(30f, ((GRID_HEIGHT - 30f) / totalGridContentH) * scrollbarTrackH);
            float thumbRatio = scrollY / maxScroll;
            float thumbY = (scrollbarTrackY + scrollbarTrackH - thumbH) - thumbRatio * (scrollbarTrackH - thumbH);

            batch.setColor(0.45f, 0.65f, 0.90f, 0.95f);
            batch.draw(whitePixel, scrollbarX, thumbY, scrollbarW, thumbH);
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * Renders crisp pixel icons for items inside their inventory slot box.
     */
    private void renderItemIcon(SpriteBatch batch, Texture whitePixel, String name, float cx, float cy) {
        String lowerName = name.toLowerCase();

        if (lowerName.contains("pen")) {
            // Pen icon: Diagonal body, cap & metal tip
            batch.setColor(0.20f, 0.45f, 0.85f, 1f); // Blue barrel
            batch.draw(whitePixel, cx - 12f, cy - 10f, 6f, 22f);
            batch.setColor(0.95f, 0.78f, 0.25f, 1f); // Gold clip/ring
            batch.draw(whitePixel, cx - 14f, cy + 4f, 10f, 3f);
            batch.setColor(0.85f, 0.85f, 0.90f, 1f); // Metal nib
            batch.draw(whitePixel, cx - 11f, cy - 14f, 4f, 4f);
        } else if (lowerName.contains("notebook")) {
            // Notebook icon: Rectangular cover & spiral spine
            batch.setColor(0.85f, 0.25f, 0.25f, 1f); // Red cover
            batch.draw(whitePixel, cx - 12f, cy - 14f, 24f, 28f);
            batch.setColor(0.95f, 0.95f, 0.95f, 1f); // Page edges
            batch.draw(whitePixel, cx + 8f, cy - 12f, 3f, 24f);
            batch.setColor(0.2f, 0.2f, 0.25f, 1f);  // Spiral rings
            for (int r = -10; r <= 10; r += 5) {
                batch.draw(whitePixel, cx - 14f, cy + r, 4f, 2f);
            }
        } else if (lowerName.contains("controller")) {
            // Game Controller icon: Gamepad body, D-pad, action buttons
            batch.setColor(0.25f, 0.28f, 0.35f, 1f); // Gamepad body
            batch.draw(whitePixel, cx - 16f, cy - 10f, 32f, 20f);
            batch.setColor(0.85f, 0.85f, 0.90f, 1f); // D-pad
            batch.draw(whitePixel, cx - 12f, cy - 3f, 8f, 3f);
            batch.draw(whitePixel, cx - 10f, cy - 5f, 3f, 7f);
            batch.setColor(0.95f, 0.35f, 0.35f, 1f); // Action buttons
            batch.draw(whitePixel, cx + 7f, cy, 3f, 3f);
            batch.setColor(0.35f, 0.85f, 0.45f, 1f);
            batch.draw(whitePixel, cx + 4f, cy - 3f, 3f, 3f);
        } else if (lowerName.contains("money")) {
            // Money icon: Stack of green banknotes with center emblem
            batch.setColor(0.18f, 0.65f, 0.30f, 1f); // Cash note
            batch.draw(whitePixel, cx - 15f, cy - 10f, 30f, 18f);
            batch.setColor(0.28f, 0.80f, 0.42f, 1f); // Inner border accent
            batch.draw(whitePixel, cx - 13f, cy - 8f, 26f, 14f);
            batch.setColor(0.95f, 0.85f, 0.3f, 1f); // Center dollar symbol
            batch.draw(whitePixel, cx - 2f, cy - 5f, 4f, 10f);
        } else {
            // Generic item crate fallback icon
            batch.setColor(0.60f, 0.40f, 0.20f, 1f);
            batch.draw(whitePixel, cx - 12f, cy - 12f, 24f, 24f);
            batch.setColor(0.85f, 0.70f, 0.30f, 1f);
            batch.draw(whitePixel, cx - 12f, cy - 12f, 24f, 3f);
            batch.draw(whitePixel, cx - 12f, cy + 9f, 24f, 3f);
            batch.draw(whitePixel, cx - 2f, cy - 12f, 4f, 24f);
        }
    }
}
