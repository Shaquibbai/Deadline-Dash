package io.github.shaquibbai.deadlinedash.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * Handles centered confirmation dialogues before storing items into the backpack
 * and renders auto-fading subtle toast notifications upon successful storage.
 */
public class ItemConfirmationDialog {
    private final Backpack backpack;
    private final GlyphLayout layout = new GlyphLayout();

    // Pending item storage state
    private Item pendingItem = null;
    private int pendingQuantity = 1;
    private boolean active = false;

    // Toast notification state
    private String toastMessage = "";
    private float toastTimer = 0f;
    private static final float TOAST_DURATION = 1.8f;
    private static final float TOAST_FADE_DURATION = 0.5f;

    // Dialog layout dimensions centered on virtual viewport (1280 x 720)
    private static final float DIALOG_W = 560f;
    private static final float DIALOG_H = 220f;
    private static final float DIALOG_X = (1280f - DIALOG_W) / 2f; // 360f (Dead-Center)
    private static final float DIALOG_Y = (720f - DIALOG_H) / 2f;  // 250f (Dead-Center)

    // Button dimensions
    private static final float BTN_W = 160f;
    private static final float BTN_H = 48f;
    private final Rectangle yesButton = new Rectangle(DIALOG_X + 75f, DIALOG_Y + 30f, BTN_W, BTN_H);
    private final Rectangle noButton = new Rectangle(DIALOG_X + DIALOG_W - 75f - BTN_W, DIALOG_Y + 30f, BTN_W, BTN_H);

    public ItemConfirmationDialog(Backpack backpack) {
        this.backpack = backpack;
    }

    /**
     * Prompts the player with a confirmation dialogue to store an item.
     */
    public void requestStoreItem(Item item, int quantity) {
        if (item == null || quantity <= 0) return;
        this.pendingItem = item;
        this.pendingQuantity = quantity;
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Displays a temporary toast notification overlay on screen.
     */
    public void showToast(String message) {
        if (message == null || message.trim().isEmpty()) return;
        this.toastMessage = message.trim();
        this.toastTimer = TOAST_DURATION;
    }

    /**
     * Updates input handling and notification timers.
     */
    public void update(float delta, Vector3 mousePos, boolean justClicked) {
        // Update toast notification timer
        if (toastTimer > 0f) {
            toastTimer -= delta;
            if (toastTimer <= 0f) {
                toastMessage = "";
            }
        }

        if (!active) return;

        // Key shortcuts (ENTER / Y -> YES, ESC / N -> NO)
        boolean yesKey = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.Y);
        boolean noKey = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.N);

        if (yesKey) {
            confirmStorage();
            return;
        }
        if (noKey) {
            cancelStorage();
            return;
        }

        // Mouse click checks
        if (justClicked && mousePos != null) {
            if (yesButton.contains(mousePos.x, mousePos.y)) {
                confirmStorage();
            } else if (noButton.contains(mousePos.x, mousePos.y)) {
                cancelStorage();
            }
        }
    }

    private void confirmStorage() {
        if (pendingItem != null && backpack != null) {
            backpack.addItem(pendingItem, pendingQuantity);
            toastMessage = pendingItem.getName() + " has been stored in the bag.";
            toastTimer = TOAST_DURATION;
        }
        active = false;
        pendingItem = null;
        pendingQuantity = 1;
    }

    private void cancelStorage() {
        active = false;
        pendingItem = null;
        pendingQuantity = 1;
    }

    /**
     * Renders the confirmation dialogue modal (if active) and toast notifications (if active).
     */
    public void render(SpriteBatch batch, BitmapFont titleFont, BitmapFont bodyFont, Texture whitePixel, Vector3 mousePos) {
        // 1. Render Toast Notification if active (Subtle centered top toast)
        if (toastTimer > 0f && !toastMessage.isEmpty()) {
            float alpha = 1.0f;
            if (toastTimer < TOAST_FADE_DURATION) {
                alpha = MathUtils.clamp(toastTimer / TOAST_FADE_DURATION, 0f, 1f);
            }

            layout.setText(bodyFont, toastMessage);
            float paddingX = 28f;
            float paddingY = 14f;
            float toastW = layout.width + paddingX * 2f;
            float toastH = layout.height + paddingY * 2f;
            float toastX = (1280f - toastW) / 2f; // Centered
            float toastY = 610f;

            // Toast background (Dark slate container, no green/screen flash)
            batch.setColor(0.10f, 0.12f, 0.17f, 0.94f * alpha);
            batch.draw(whitePixel, toastX, toastY, toastW, toastH);

            // Toast border accent (Subtle blue-gray border)
            batch.setColor(0.35f, 0.48f, 0.65f, 0.90f * alpha);
            batch.draw(whitePixel, toastX, toastY, toastW, 2);
            batch.draw(whitePixel, toastX, toastY + toastH - 2, toastW, 2);
            batch.draw(whitePixel, toastX, toastY, 2, toastH);
            batch.draw(whitePixel, toastX + toastW - 2, toastY, 2, toastH);

            // Toast text
            bodyFont.setColor(0.95f, 0.95f, 0.98f, alpha);
            bodyFont.draw(batch, toastMessage, toastX + paddingX, toastY + toastH - paddingY + 2f);
        }

        // 2. Render Confirmation Modal if active
        if (!active) return;

        // Dark dim backdrop overlay
        batch.setColor(0f, 0f, 0f, 0.65f);
        batch.draw(whitePixel, 0, 0, 1280, 720);

        // Dialogue Container Panel (Centered on screen)
        batch.setColor(0.10f, 0.12f, 0.17f, 0.97f);
        batch.draw(whitePixel, DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H);

        // Top Accent Strip
        batch.setColor(0.90f, 0.70f, 0.20f, 1.0f);
        batch.draw(whitePixel, DIALOG_X, DIALOG_Y + DIALOG_H - 5f, DIALOG_W, 5f);

        // Panel Border
        batch.setColor(0.35f, 0.48f, 0.65f, 0.90f);
        batch.draw(whitePixel, DIALOG_X, DIALOG_Y, DIALOG_W, 2);
        batch.draw(whitePixel, DIALOG_X, DIALOG_Y + DIALOG_H - 2, DIALOG_W, 2);
        batch.draw(whitePixel, DIALOG_X, DIALOG_Y, 2, DIALOG_H);
        batch.draw(whitePixel, DIALOG_X + DIALOG_W - 2, DIALOG_Y, 2, DIALOG_H);

        // Dialogue Question Text: "Wanna store this in your backpack?"
        String questionText = "Wanna store this in your backpack?";
        titleFont.setColor(Color.WHITE);
        layout.setText(titleFont, questionText);
        titleFont.draw(batch, questionText, DIALOG_X + (DIALOG_W - layout.width) / 2f, DIALOG_Y + DIALOG_H - 45f);

        // Subtext showing item name & quantity
        if (pendingItem != null) {
            String itemDetail = pendingItem.getName() + " x " + pendingQuantity;
            bodyFont.setColor(new Color(1.0f, 0.85f, 0.3f, 1f));
            layout.setText(bodyFont, itemDetail);
            bodyFont.draw(batch, itemDetail, DIALOG_X + (DIALOG_W - layout.width) / 2f, DIALOG_Y + DIALOG_H - 95f);
        }

        // Render YES / NO Buttons
        boolean yesHover = mousePos != null && yesButton.contains(mousePos.x, mousePos.y);
        boolean noHover = mousePos != null && noButton.contains(mousePos.x, mousePos.y);

        renderButton(batch, bodyFont, whitePixel, yesButton, "YES", yesHover, new Color(0.20f, 0.75f, 0.35f, 1f));
        renderButton(batch, bodyFont, whitePixel, noButton, "NO", noHover, new Color(0.85f, 0.30f, 0.30f, 1f));
        batch.setColor(Color.WHITE);
    }

    private void renderButton(SpriteBatch batch, BitmapFont font, Texture whitePixel, Rectangle rect, String label, boolean hover, Color color) {
        float x = rect.x;
        float y = rect.y;
        float w = rect.width;
        float h = rect.height;

        if (hover) {
            batch.setColor(color.r, color.g, color.b, 0.95f);
        } else {
            batch.setColor(0.16f, 0.20f, 0.26f, 0.92f);
        }
        batch.draw(whitePixel, x, y, w, h);

        batch.setColor(hover ? Color.WHITE : color);
        batch.draw(whitePixel, x, y, w, 2);
        batch.draw(whitePixel, x, y + h - 2, w, 2);
        batch.draw(whitePixel, x, y, 2, h);
        batch.draw(whitePixel, x + w - 2, y, 2, h);

        font.setColor(Color.WHITE);
        layout.setText(font, label);
        font.draw(batch, label, x + (w - layout.width) / 2f, y + (h + layout.height) / 2f);
    }
}
