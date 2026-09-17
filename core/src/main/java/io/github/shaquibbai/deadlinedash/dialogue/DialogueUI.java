package io.github.shaquibbai.deadlinedash.dialogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/**
 * Screen-space HUD renderer for active NPC and player dialogues.
 * Renders an anchored bottom dialog panel, speaker name badge, wrapped dialogue lines,
 * and an animated [F] progression prompt.
 */
public class DialogueUI {
    // Screen-space dimensions (Viewport: 1280 x 720)
    private static final float PANEL_W = 1040f;
    private static final float PANEL_H = 175f;
    private static final float PANEL_X = (1280f - PANEL_W) / 2f; // 120f
    private static final float PANEL_Y = 28f;

    private static final float HEADER_H = 38f;
    private static final float PADDING_X = 28f;

    private final GlyphLayout layout = new GlyphLayout();
    private float animTimer = 0f;

    public void update(float delta) {
        animTimer += delta;
    }

    /**
     * Renders the dialogue UI if dialogue is active.
     */
    public void render(SpriteBatch batch, BitmapFont speakerFont, BitmapFont bodyFont, Texture whitePixel, DialogueManager dialogueManager) {
        if (dialogueManager == null || !dialogueManager.isActive()) {
            return;
        }

        // 1. Panel Container Background
        batch.setColor(0.09f, 0.11f, 0.16f, 0.95f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // Header Background Bar
        batch.setColor(0.14f, 0.18f, 0.25f, 0.98f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, HEADER_H);

        // Top Accent Strip (Gold for NPC, Cyan for Player)
        boolean isPlayer = dialogueManager.getCurrentSpeakerType() == DialogueSpeaker.PLAYER;
        if (isPlayer) {
            batch.setColor(0.30f, 0.85f, 1.0f, 1.0f);
        } else {
            batch.setColor(0.95f, 0.78f, 0.25f, 1.0f);
        }
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 3f, PANEL_W, 3f);

        // Outer Border
        batch.setColor(0.30f, 0.45f, 0.65f, 0.90f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 2, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, 2, PANEL_H);
        batch.draw(whitePixel, PANEL_X + PANEL_W - 2, PANEL_Y, 2, PANEL_H);

        // Header Separator Line
        batch.setColor(0.25f, 0.35f, 0.50f, 0.85f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, 1);

        // 2. Speaker Name
        String speaker = dialogueManager.getCurrentSpeaker();
        if (speaker != null && !speaker.isEmpty()) {
            if (isPlayer) {
                speakerFont.setColor(new Color(0.40f, 0.88f, 1.0f, 1.0f));
            } else {
                speakerFont.setColor(new Color(1.0f, 0.85f, 0.30f, 1.0f));
            }
            speakerFont.draw(batch, speaker, PANEL_X + PADDING_X, PANEL_Y + PANEL_H - 12f);
        }

        // 3. Dialogue Line (Wrapped)
        String line = dialogueManager.getCurrentLine();
        if (line != null && !line.isEmpty()) {
            float textWrapWidth = PANEL_W - (PADDING_X * 2f);
            bodyFont.setColor(new Color(0.95f, 0.95f, 0.98f, 1.0f));
            layout.setText(bodyFont, line, bodyFont.getColor(), textWrapWidth, Align.left, true);
            bodyFont.draw(batch, layout, PANEL_X + PADDING_X, PANEL_Y + PANEL_H - HEADER_H - 18f);
        }

        // 4. Prompt Indicator [F] NEXT > / [F] CLOSE > (Bottom Right)
        float pulse = (float) Math.sin(animTimer * 6f) * 0.25f + 0.75f;
        bodyFont.setColor(0.70f, 0.85f, 1.0f, pulse);

        String promptText = dialogueManager.isLastLine() ? "[F] CLOSE >" : "[F] NEXT >";
        layout.setText(bodyFont, promptText);
        bodyFont.draw(batch, promptText, PANEL_X + PANEL_W - layout.width - 24f, PANEL_Y + 22f);

        batch.setColor(Color.WHITE);
    }

    // YES / NO Button bounds for confirmation mode
    public static final float BTN_W = 100f;
    public static final float BTN_H = 38f;
    public static final float YES_BTN_X = PANEL_X + PANEL_W - 230f;
    public static final float YES_BTN_Y = PANEL_Y + 12f;
    public static final float NO_BTN_X = PANEL_X + PANEL_W - 115f;
    public static final float NO_BTN_Y = PANEL_Y + 12f;

    public static final com.badlogic.gdx.math.Rectangle YES_BUTTON_BOUNDS = new com.badlogic.gdx.math.Rectangle(YES_BTN_X, YES_BTN_Y, BTN_W, BTN_H);
    public static final com.badlogic.gdx.math.Rectangle NO_BUTTON_BOUNDS = new com.badlogic.gdx.math.Rectangle(NO_BTN_X, NO_BTN_Y, BTN_W, BTN_H);

    /**
     * Renders a map transition confirmation dialog reusing the exact visual panel style,
     * borders, header, and fonts of the existing NPC dialogue window, with clickable YES / NO buttons.
     */
    public void renderConfirmation(SpriteBatch batch, BitmapFont bodyFont, Texture whitePixel, String promptMessage, com.badlogic.gdx.math.Vector3 mousePos) {
        if (promptMessage == null || promptMessage.isEmpty()) {
            return;
        }

        // 1. Panel Container Background (Same as NPC dialogue UI)
        batch.setColor(0.09f, 0.11f, 0.16f, 0.95f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // Header Background Bar
        batch.setColor(0.14f, 0.18f, 0.25f, 0.98f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, HEADER_H);

        // Top Accent Strip (Teal / Cyan accent for system transition prompts)
        batch.setColor(0.30f, 0.85f, 1.0f, 1.0f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 3f, PANEL_W, 3f);

        // Outer Border
        batch.setColor(0.30f, 0.45f, 0.65f, 0.90f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 2, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, 2, PANEL_H);
        batch.draw(whitePixel, PANEL_X + PANEL_W - 2, PANEL_Y, 2, PANEL_H);

        // Header Separator Line
        batch.setColor(0.25f, 0.35f, 0.50f, 0.85f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, 1);

        // 2. Confirmation Prompt Text (Wrapped)
        float textWrapWidth = PANEL_W - (PADDING_X * 2f);
        bodyFont.setColor(new Color(0.95f, 0.95f, 0.98f, 1.0f));
        layout.setText(bodyFont, promptMessage, bodyFont.getColor(), textWrapWidth, Align.left, true);
        bodyFont.draw(batch, layout, PANEL_X + PADDING_X, PANEL_Y + PANEL_H - HEADER_H - 24f);

        // 3. Clickable YES and NO Buttons (Bottom Right)
        boolean yesHovered = mousePos != null && YES_BUTTON_BOUNDS.contains(mousePos.x, mousePos.y);
        boolean noHovered = mousePos != null && NO_BUTTON_BOUNDS.contains(mousePos.x, mousePos.y);

        drawConfirmationButton(batch, bodyFont, whitePixel, "YES", YES_BUTTON_BOUNDS, yesHovered, new Color(0.20f, 0.70f, 0.45f, 1f));
        drawConfirmationButton(batch, bodyFont, whitePixel, "NO", NO_BUTTON_BOUNDS, noHovered, new Color(0.85f, 0.25f, 0.25f, 1f));

        batch.setColor(Color.WHITE);
    }

    private void drawConfirmationButton(SpriteBatch batch, BitmapFont font, Texture whitePixel, String text, com.badlogic.gdx.math.Rectangle bounds, boolean isHovered, Color accentColor) {
        // Button background
        if (isHovered) {
            batch.setColor(accentColor.r, accentColor.g, accentColor.b, 0.90f);
        } else {
            batch.setColor(0.12f, 0.15f, 0.22f, 0.90f);
        }
        batch.draw(whitePixel, bounds.x, bounds.y, bounds.width, bounds.height);

        // Button border
        batch.setColor(isHovered ? Color.WHITE : accentColor);
        batch.draw(whitePixel, bounds.x, bounds.y, bounds.width, 2);
        batch.draw(whitePixel, bounds.x, bounds.y + bounds.height - 2, bounds.width, 2);
        batch.draw(whitePixel, bounds.x, bounds.y, 2, bounds.height);
        batch.draw(whitePixel, bounds.x + bounds.width - 2, bounds.y, 2, bounds.height);

        // Button text
        font.setColor(isHovered ? Color.WHITE : new Color(0.92f, 0.92f, 0.96f, 1f));
        layout.setText(font, text);
        font.draw(batch, text, bounds.x + (bounds.width - layout.width) / 2f, bounds.y + (bounds.height + layout.height) / 2f);
    }

    /**
     * Renders a modal alert for when an intruder is identified.
     */
    public void renderIntruderAlert(SpriteBatch batch, BitmapFont titleFont, BitmapFont bodyFont, Texture whitePixel, String npcName, int regNumber, int foundCount) {
        float alertW = 680f;
        float alertH = 200f;
        float alertX = (1280f - alertW) / 2f;
        float alertY = (720f - alertH) / 2f;
        float alertHeaderH = 42f;

        // Dim background overlay
        batch.setColor(0f, 0f, 0f, 0.65f);
        batch.draw(whitePixel, 0, 0, 1280f, 720f);

        // Container background
        batch.setColor(0.12f, 0.08f, 0.08f, 0.96f);
        batch.draw(whitePixel, alertX, alertY, alertW, alertH);

        // Header Background
        batch.setColor(0.25f, 0.08f, 0.08f, 0.98f);
        batch.draw(whitePixel, alertX, alertY + alertH - alertHeaderH, alertW, alertHeaderH);

        // Top Accent (Bright Crimson Red)
        batch.setColor(1.0f, 0.20f, 0.20f, 1.0f);
        batch.draw(whitePixel, alertX, alertY + alertH - 3f, alertW, 3f);

        // Outer Border
        batch.setColor(0.85f, 0.25f, 0.25f, 0.90f);
        batch.draw(whitePixel, alertX, alertY, alertW, 2);
        batch.draw(whitePixel, alertX, alertY + alertH - 2, alertW, 2);
        batch.draw(whitePixel, alertX, alertY, 2, alertH);
        batch.draw(whitePixel, alertX + alertW - 2, alertY, 2, alertH);

        // Header Title
        titleFont.setColor(new Color(1.0f, 0.35f, 0.35f, 1.0f));
        layout.setText(titleFont, "🚨 INTRUDER DETECTED!");
        titleFont.draw(batch, "🚨 INTRUDER DETECTED!", alertX + 24f, alertY + alertH - 12f);

        // Body Text
        bodyFont.setColor(new Color(0.95f, 0.95f, 0.98f, 1.0f));
        String line1 = "Target: " + npcName + "   |   Registration No: " + regNumber + " (ODD NUMBER)";
        String line2 = "Progress: " + foundCount + " / 5 Intruders Identified (" + (5 - foundCount) + " remaining)";
        bodyFont.draw(batch, line1, alertX + 24f, alertY + alertH - alertHeaderH - 24f);
        bodyFont.draw(batch, line2, alertX + 24f, alertY + alertH - alertHeaderH - 54f);

        // Animated Prompt [ENTER / F]
        float pulse = (float) Math.sin(animTimer * 6f) * 0.25f + 0.75f;
        bodyFont.setColor(1.0f, 0.85f, 0.40f, pulse);
        String prompt = "[ENTER / F] DISMISS >";
        layout.setText(bodyFont, prompt);
        bodyFont.draw(batch, prompt, alertX + alertW - layout.width - 24f, alertY + 22f);

        batch.setColor(Color.WHITE);
    }

    /**
     * Renders a celebration modal banner when all 5 intruders have been found.
     */
    public void renderQuestCompletionBanner(SpriteBatch batch, BitmapFont titleFont, BitmapFont bodyFont, Texture whitePixel) {
        float bannerW = 740f;
        float bannerH = 220f;
        float bannerX = (1280f - bannerW) / 2f;
        float bannerY = (720f - bannerH) / 2f;
        float bannerHeaderH = 46f;

        // Dim background overlay
        batch.setColor(0f, 0f, 0f, 0.70f);
        batch.draw(whitePixel, 0, 0, 1280f, 720f);

        // Container background
        batch.setColor(0.09f, 0.12f, 0.16f, 0.96f);
        batch.draw(whitePixel, bannerX, bannerY, bannerW, bannerH);

        // Header Background
        batch.setColor(0.14f, 0.20f, 0.28f, 0.98f);
        batch.draw(whitePixel, bannerX, bannerY + bannerH - bannerHeaderH, bannerW, bannerHeaderH);

        // Top Accent (Gold / Yellow)
        batch.setColor(0.95f, 0.78f, 0.25f, 1.0f);
        batch.draw(whitePixel, bannerX, bannerY + bannerH - 3f, bannerW, 3f);

        // Outer Border
        batch.setColor(0.95f, 0.78f, 0.25f, 0.90f);
        batch.draw(whitePixel, bannerX, bannerY, bannerW, 2);
        batch.draw(whitePixel, bannerX, bannerY + bannerH - 2, bannerW, 2);
        batch.draw(whitePixel, bannerX, bannerY, 2, bannerH);
        batch.draw(whitePixel, bannerX + bannerW - 2, bannerY, 2, bannerH);

        // Header Title
        titleFont.setColor(new Color(1.0f, 0.88f, 0.25f, 1.0f));
        titleFont.draw(batch, "🏆 QUEST COMPLETE — INTRUDER HUNT", bannerX + 24f, bannerY + bannerH - 14f);

        // Body Text
        bodyFont.setColor(new Color(0.35f, 0.95f, 0.55f, 1.0f));
        bodyFont.draw(batch, "✅ ALL 5 INTRUDERS FOUND!", bannerX + 24f, bannerY + bannerH - bannerHeaderH - 24f);

        bodyFont.setColor(new Color(0.92f, 0.92f, 0.96f, 1.0f));
        bodyFont.draw(batch, "All unauthorized individuals have been reported to security.", bannerX + 24f, bannerY + bannerH - bannerHeaderH - 52f);
        bodyFont.draw(batch, "Reward: +35 REP awarded to Talha.", bannerX + 24f, bannerY + bannerH - bannerHeaderH - 76f);

        // Animated Prompt [ESC / ENTER]
        float pulse = (float) Math.sin(animTimer * 6f) * 0.25f + 0.75f;
        bodyFont.setColor(0.70f, 0.85f, 1.0f, pulse);
        String prompt = "[ESC / ENTER] BACK TO MAP >";
        layout.setText(bodyFont, prompt);
        bodyFont.draw(batch, prompt, bannerX + bannerW - layout.width - 24f, bannerY + 22f);

        batch.setColor(Color.WHITE);
    }

    public void dispose() {
    }
}

