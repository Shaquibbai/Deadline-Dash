package io.github.shaquibbai.deadlinedash.ending;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.shaquibbai.deadlinedash.DeadlineDash;
import io.github.shaquibbai.deadlinedash.assets.AssetPaths;
import io.github.shaquibbai.deadlinedash.screen.TitleScreen;

/**
 * Screen displaying the ending cinematic for Deadline Dash.
 * Renders the uncropped classroom scene, character dialogues between Teacher and Shaquib,
 * smooth black transitions, and the final centered message card before returning to TitleScreen.
 */
public class EndingScreen implements Screen {

    public static class DialogueEntry {
        public final String speaker;
        public final String text;

        public DialogueEntry(String speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
    }

    private enum EndingPhase {
        FADE_IN_SCENE,
        DIALOGUE,
        FADE_TO_BLACK,
        FINAL_CARD,
        FADE_TO_TITLE
    }

    private final DeadlineDash game;
    private final SpriteBatch batch;
    private final EndingType endingType;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture classroomTexture;
    private Texture whitePixel;
    private BitmapFont speakerFont;
    private BitmapFont dialogueFont;
    private BitmapFont cardFont;
    private BitmapFont promptFont;

    private final GlyphLayout layout = new GlyphLayout();

    private final DialogueEntry[] dialogueLines;
    private final String finalCardMessage;
    private int currentDialogueIndex = 0;

    private EndingPhase phase = EndingPhase.FADE_IN_SCENE;
    private float phaseTimer = 0f;
    private float animTimer = 0f;
    private float inputCooldown = 0.35f;

    private static final float FADE_IN_SCENE_DURATION = 0.6f;
    private static final float FADE_TO_BLACK_DURATION = 0.8f;
    private static final float FADE_TO_TITLE_DURATION = 0.6f;

    // Dialogue Panel dimensions matching game's DialogueUI
    private static final float PANEL_W = 1040f;
    private static final float PANEL_H = 175f;
    private static final float PANEL_X = (1280f - PANEL_W) / 2f;
    private static final float PANEL_Y = 28f;
    private static final float HEADER_H = 38f;
    private static final float PADDING_X = 28f;

    public EndingScreen(DeadlineDash game, EndingType endingType) {
        this.game = game;
        this.batch = game.getBatch();
        this.endingType = endingType;

        if (endingType == EndingType.WIN) {
            this.dialogueLines = new DialogueEntry[] {
                new DialogueEntry("Teacher", "That's great. how did you guys pull it off??"),
                new DialogueEntry("Shaquib", "well, we needed to grind a lot last few days."),
                new DialogueEntry("Teacher", "It worked, you all are getting A+.")
            };
            this.finalCardMessage = "well done, you can explore the campus by playing again!!!";
        } else {
            this.dialogueLines = new DialogueEntry[] {
                new DialogueEntry("Teacher", "I know i shifted the time but how come only you guys didnt finish??"),
                new DialogueEntry("Shaquib", "..........."),
                new DialogueEntry("Teacher", "Well whatever you will get what you did...NOTHING"),
                new DialogueEntry("Shaquib", "........")
            };
            this.finalCardMessage = "you should try to finish faster next time";
        }

        initViewport();
        initAssets();
    }

    private void initViewport() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280f, 720f, camera);
        viewport.apply();
        camera.position.set(640f, 360f, 0f);
        camera.update();
    }

    private void initAssets() {
        classroomTexture = new Texture(Gdx.files.internal(AssetPaths.ENDING_SCENE_IMAGE));
        classroomTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        speakerFont = new BitmapFont();
        speakerFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        speakerFont.getData().setScale(1.4f);

        dialogueFont = new BitmapFont();
        dialogueFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dialogueFont.getData().setScale(1.35f);

        cardFont = new BitmapFont();
        cardFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cardFont.getData().setScale(1.85f);

        promptFont = new BitmapFont();
        promptFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        promptFont.getData().setScale(1.0f);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        animTimer += delta;
        phaseTimer += delta;
        if (inputCooldown > 0f) {
            inputCooldown -= delta;
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        switch (phase) {
            case FADE_IN_SCENE:
                drawClassroomBackground();
                drawDialogueBox();
                float fadeInAlpha = 1.0f - MathUtils.clamp(phaseTimer / FADE_IN_SCENE_DURATION, 0f, 1f);
                if (fadeInAlpha > 0f) {
                    batch.setColor(0f, 0f, 0f, fadeInAlpha);
                    batch.draw(whitePixel, 0, 0, 1280, 720);
                }
                if (phaseTimer >= FADE_IN_SCENE_DURATION) {
                    phase = EndingPhase.DIALOGUE;
                    phaseTimer = 0f;
                }
                break;

            case DIALOGUE:
                drawClassroomBackground();
                drawDialogueBox();
                break;

            case FADE_TO_BLACK:
                drawClassroomBackground();
                drawDialogueBox();
                float fadeBlackAlpha = MathUtils.clamp(phaseTimer / FADE_TO_BLACK_DURATION, 0f, 1f);
                batch.setColor(0f, 0f, 0f, fadeBlackAlpha);
                batch.draw(whitePixel, 0, 0, 1280, 720);
                if (phaseTimer >= FADE_TO_BLACK_DURATION) {
                    phase = EndingPhase.FINAL_CARD;
                    phaseTimer = 0f;
                    inputCooldown = 0.4f;
                }
                break;

            case FINAL_CARD:
                drawFinalCard();
                break;

            case FADE_TO_TITLE:
                drawFinalCard();
                float fadeTitleAlpha = MathUtils.clamp(phaseTimer / FADE_TO_TITLE_DURATION, 0f, 1f);
                batch.setColor(0f, 0f, 0f, fadeTitleAlpha);
                batch.draw(whitePixel, 0, 0, 1280, 720);
                if (phaseTimer >= FADE_TO_TITLE_DURATION) {
                    transitionToTitle();
                    batch.end();
                    return;
                }
                break;
        }

        batch.end();

        handleInput();
    }

    private void drawClassroomBackground() {
        float texW = classroomTexture.getWidth();
        float texH = classroomTexture.getHeight();
        float viewW = 1280f;
        float viewH = 720f;

        // Scale to fit within 1280x720 without cropping or distorting
        float scale = Math.min(viewW / texW, viewH / texH);
        float drawW = texW * scale;
        float drawH = texH * scale;
        float drawX = (viewW - drawW) / 2f;
        float drawY = (viewH - drawH) / 2f;

        batch.setColor(Color.WHITE);
        batch.draw(classroomTexture, drawX, drawY, drawW, drawH);
    }

    private void drawDialogueBox() {
        if (currentDialogueIndex >= dialogueLines.length) return;

        DialogueEntry entry = dialogueLines[currentDialogueIndex];
        boolean isShaquib = "Shaquib".equalsIgnoreCase(entry.speaker);

        // 1. Panel Container Background
        batch.setColor(0.09f, 0.11f, 0.16f, 0.95f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // 2. Header Background Bar
        batch.setColor(0.14f, 0.18f, 0.25f, 0.98f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, HEADER_H);

        // 3. Top Accent Strip (Gold for Teacher, Cyan for Shaquib)
        if (isShaquib) {
            batch.setColor(0.30f, 0.85f, 1.0f, 1.0f);
        } else {
            batch.setColor(0.95f, 0.78f, 0.25f, 1.0f);
        }
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 3f, PANEL_W, 3f);

        // 4. Outer Border
        batch.setColor(0.30f, 0.45f, 0.65f, 0.90f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - 2, PANEL_W, 2);
        batch.draw(whitePixel, PANEL_X, PANEL_Y, 2, PANEL_H);
        batch.draw(whitePixel, PANEL_X + PANEL_W - 2, PANEL_Y, 2, PANEL_H);

        // 5. Header Separator Line
        batch.setColor(0.25f, 0.35f, 0.50f, 0.85f);
        batch.draw(whitePixel, PANEL_X, PANEL_Y + PANEL_H - HEADER_H, PANEL_W, 1);

        // 6. Speaker Name
        if (isShaquib) {
            speakerFont.setColor(new Color(0.40f, 0.88f, 1.0f, 1.0f));
        } else {
            speakerFont.setColor(new Color(1.0f, 0.85f, 0.30f, 1.0f));
        }
        speakerFont.draw(batch, entry.speaker, PANEL_X + PADDING_X, PANEL_Y + PANEL_H - 12f);

        // 7. Dialogue Line (Wrapped)
        float textWrapWidth = PANEL_W - (PADDING_X * 2f);
        dialogueFont.setColor(new Color(0.95f, 0.95f, 0.98f, 1.0f));
        layout.setText(dialogueFont, entry.text, dialogueFont.getColor(), textWrapWidth, Align.left, true);
        dialogueFont.draw(batch, layout, PANEL_X + PADDING_X, PANEL_Y + PANEL_H - HEADER_H - 18f);

        // 8. Prompt Indicator
        float pulse = (float) Math.sin(animTimer * 6f) * 0.25f + 0.75f;
        promptFont.setColor(0.70f, 0.85f, 1.0f, pulse);

        boolean isLast = currentDialogueIndex == dialogueLines.length - 1;
        String promptText = isLast ? "[SPACE / CLICK] FINISH >" : "[SPACE / CLICK] NEXT >";
        layout.setText(promptFont, promptText);
        promptFont.draw(batch, promptText, PANEL_X + PANEL_W - layout.width - 24f, PANEL_Y + 22f);

        batch.setColor(Color.WHITE);
    }

    private void drawFinalCard() {
        // Pure black screen is already cleared
        float wrapWidth = 960f;

        // Centered final message card
        if (endingType == EndingType.WIN) {
            cardFont.setColor(new Color(0.35f, 0.95f, 0.65f, 1.0f)); // Celebratory Green
        } else {
            cardFont.setColor(new Color(1.0f, 0.45f, 0.45f, 1.0f)); // Soft Red/Coral
        }

        layout.setText(cardFont, finalCardMessage, cardFont.getColor(), wrapWidth, Align.center, true);
        float textX = (1280f - wrapWidth) / 2f;
        float textY = (720f + layout.height) / 2f;
        cardFont.draw(batch, layout, textX, textY);

        // Bottom prompt to return to title
        float pulse = (float) Math.sin(animTimer * 5f) * 0.3f + 0.7f;
        promptFont.setColor(0.75f, 0.75f, 0.85f, pulse);
        layout.setText(promptFont, "PRESS [SPACE / ENTER / CLICK] TO CONTINUE");
        promptFont.draw(batch, layout, (1280f - layout.width) / 2f, 75f);
    }

    private void handleInput() {
        if (inputCooldown > 0f) return;

        boolean spacePressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean enterPressed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        boolean mouseClicked = Gdx.input.justTouched();

        if (spacePressed || enterPressed || mouseClicked) {
            if (phase == EndingPhase.DIALOGUE) {
                currentDialogueIndex++;
                inputCooldown = 0.22f;
                if (currentDialogueIndex >= dialogueLines.length) {
                    phase = EndingPhase.FADE_TO_BLACK;
                    phaseTimer = 0f;
                }
            } else if (phase == EndingPhase.FINAL_CARD) {
                phase = EndingPhase.FADE_TO_TITLE;
                phaseTimer = 0f;
                inputCooldown = 1.0f;
            }
        }
    }

    private void transitionToTitle() {
        game.setScreen(new TitleScreen(game));
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(640f, 360f, 0f);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (classroomTexture != null) classroomTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (speakerFont != null) speakerFont.dispose();
        if (dialogueFont != null) dialogueFont.dispose();
        if (cardFont != null) cardFont.dispose();
        if (promptFont != null) promptFont.dispose();
    }

    public EndingType getEndingType() {
        return endingType;
    }

    public int getCurrentDialogueIndex() {
        return currentDialogueIndex;
    }

    public DialogueEntry[] getDialogueLines() {
        return dialogueLines;
    }

    public String getFinalCardMessage() {
        return finalCardMessage;
    }
}
