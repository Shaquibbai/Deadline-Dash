package io.github.shaquibbai.deadlinedash.screen;

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

/**
 * Opening Sequence and Objective Cards Screen.
 * Renders minimalist black-background cinematic text (without dialogue boxes),
 * phone notification, group chat UI, scream effects, and objective cards.
 */
public class OpeningScreen implements Screen {
    private final DeadlineDash game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture whitePixel;
    private BitmapFont dialogueFont;
    private BitmapFont titleFont;
    private BitmapFont chatFont;
    private BitmapFont subFont;

    private final GlyphLayout layout = new GlyphLayout();

    public enum SceneState {
        SCENE_1_SLEEPING,
        SCENE_2_WAKING,
        SCENE_3_NOTIFICATION,
        SCENE_4_REACTION,
        SCENE_5_DECISION,
        SCENE_6_GROUP_CHAT_SENT,
        SCENE_7_TIME_PASSES,
        SCENE_8_PART_DONE,
        SCENE_9_GROUP_CHAT_UNSEEN,
        SCENE_10_SCREAM,
        CARD_1_2_DAYS,
        CARD_2_FIND_TEAMMATES,
        CARD_3_FINISH_PROJECT
    }

    private enum TransitionPhase {
        NONE,
        FADE_OUT,
        HOLD_BLACK,
        FADE_IN
    }

    private SceneState currentState = SceneState.SCENE_1_SLEEPING;
    private SceneState nextState = null;

    private TransitionPhase transitionPhase = TransitionPhase.NONE;
    private float transitionTimer = 0f;

    // Transition timing constants (in seconds)
    private static final float FADE_OUT_DURATION = 0.32f;
    private static final float HOLD_BLACK_DURATION = 0.10f;
    private static final float FADE_IN_DURATION = 0.32f;

    private float animTimer = 0f;
    private float sceneTimer = 0f;

    private float shakeIntensity = 0f;
    private float flashAlpha = 0f;

    private final com.badlogic.gdx.math.Rectangle skipButtonBounds = new com.badlogic.gdx.math.Rectangle(1115f, 25f, 140f, 40f);
    private final com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3();

    public OpeningScreen(DeadlineDash game) {
        this.game = game;
        this.batch = game.getBatch();

        initViewport();
        initAssets();

        // Start initial scene with a smooth fade in
        startFadeIn();
    }

    private void initViewport() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280f, 720f, camera);
        viewport.apply();
        camera.position.set(640f, 360f, 0f);
        camera.update();
    }

    private void initAssets() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        dialogueFont = new BitmapFont();
        dialogueFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dialogueFont.getData().setScale(1.6f);
        dialogueFont.getData().markupEnabled = true;

        titleFont = new BitmapFont();
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        titleFont.getData().setScale(2.5f);
        titleFont.getData().markupEnabled = true;

        chatFont = new BitmapFont();
        chatFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        chatFont.getData().setScale(1.3f);
        chatFont.getData().markupEnabled = true;

        subFont = new BitmapFont();
        subFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        subFont.getData().setScale(1.0f);
        subFont.getData().markupEnabled = true;
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        animTimer += delta;
        sceneTimer += delta;

        // Update Fade Transitions
        updateTransition(delta);

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Calculate unprojected mouse position
        mousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);

        // Apply screen shake if active (Scream scene)
        float offsetX = 0f;
        float offsetY = 0f;
        if (shakeIntensity > 0f) {
            shakeIntensity = Math.max(0f, shakeIntensity - delta * 12f);
            offsetX = MathUtils.random(-shakeIntensity, shakeIntensity);
            offsetY = MathUtils.random(-shakeIntensity, shakeIntensity);
        }

        camera.position.set(640f + offsetX, 360f + offsetY, 0f);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Render Active Scene Content
        if (transitionPhase != TransitionPhase.HOLD_BLACK) {
            renderSceneContent();
        }

        // Draw Scream Flash overlay
        if (flashAlpha > 0f) {
            flashAlpha = Math.max(0f, flashAlpha - delta * 2.5f);
            batch.setColor(1f, 0.1f, 0.1f, flashAlpha * 0.35f);
            batch.draw(whitePixel, 0, 0, 1280, 720);
        }

        // Draw Skip Button (Bottom-Right)
        drawSkipButton();

        // Draw Global Black Fade Overlay for Smooth Transitions
        float fadeAlpha = getTransitionFadeAlpha();
        if (fadeAlpha > 0f) {
            batch.setColor(0f, 0f, 0f, MathUtils.clamp(fadeAlpha, 0f, 1f));
            batch.draw(whitePixel, 0, 0, 1280, 720);
        }

        batch.end();

        // Handle Input & Automatic Scene Progression
        handleInput(delta);
    }

    private void renderSceneContent() {
        switch (currentState) {
            case SCENE_1_SLEEPING:
                drawCinematicText("zZzzz zzZZZzzzz");
                break;
            case SCENE_2_WAKING:
                drawCinematicText("who's texting now!?");
                break;
            case SCENE_3_NOTIFICATION:
                drawPhoneNotification();
                break;
            case SCENE_4_REACTION:
                drawCinematicText("What ..........inconvenience my a!?$%^&");
                break;
            case SCENE_5_DECISION:
                drawCinematicText("Agghhh , whatever. Let me tell those morons..");
                break;
            case SCENE_6_GROUP_CHAT_SENT:
                drawGroupChat(false);
                break;
            case SCENE_7_TIME_PASSES:
                drawTimePassesScreen();
                break;
            case SCENE_8_PART_DONE:
                drawCinematicText("At last!! My part is done. Lets see how much they have done");
                break;
            case SCENE_9_GROUP_CHAT_UNSEEN:
                drawGroupChat(true);
                break;
            case SCENE_10_SCREAM:
                drawScreamScene();
                break;
            case CARD_1_2_DAYS:
                drawObjectiveCard("2 DAYS", new Color(1.0f, 0.25f, 0.25f, 1f));
                break;
            case CARD_2_FIND_TEAMMATES:
                drawObjectiveCard("FIND YOUR TEAMMATES", new Color(1.0f, 0.85f, 0.25f, 1f));
                break;
            case CARD_3_FINISH_PROJECT:
                drawObjectiveCard("FINISH THE PROJECT", new Color(0.3f, 0.9f, 1.0f, 1f));
                break;
        }
    }

    private void drawCinematicText(String text) {
        float wrapWidth = 960f;
        dialogueFont.setColor(Color.WHITE);
        layout.setText(dialogueFont, text, Color.WHITE, wrapWidth, Align.center, true);

        float drawX = (1280f - wrapWidth) / 2f;
        float drawY = (720f + layout.height) / 2f;

        dialogueFont.draw(batch, layout, drawX, drawY);

        // Advance prompt centered at bottom
        if (transitionPhase == TransitionPhase.NONE) {
            float pulse = (float) Math.sin(animTimer * 5f) * 0.3f + 0.7f;
            subFont.setColor(0.65f, 0.65f, 0.65f, pulse);
            layout.setText(subFont, "PRESS [SPACE / CLICK] TO ADVANCE >");
            subFont.draw(batch, layout, (1280f - layout.width) / 2f, 60f);
        }
    }

    private void drawPhoneNotification() {
        float notifW = 760f;
        float notifH = 260f;

        float scaleProgress = MathUtils.clamp(sceneTimer / 0.3f, 0f, 1f);
        float currentScale = 0.95f + 0.05f * scaleProgress;

        float scaledW = notifW * currentScale;
        float scaledH = notifH * currentScale;
        float drawX = (1280f - scaledW) / 2f;
        float drawY = (720f - scaledH) / 2f;

        // Smartphone notification card container
        batch.setColor(0.11f, 0.13f, 0.17f, 0.96f);
        batch.draw(whitePixel, drawX, drawY, scaledW, scaledH);

        // Top accent stripe (Warning Red/Orange)
        batch.setColor(0.95f, 0.3f, 0.2f, 1f);
        batch.draw(whitePixel, drawX, drawY + scaledH - 6f, scaledW, 6f);

        // Card border
        batch.setColor(0.3f, 0.35f, 0.45f, 0.8f);
        batch.draw(whitePixel, drawX, drawY, scaledW, 2);
        batch.draw(whitePixel, drawX, drawY, 2, scaledH);
        batch.draw(whitePixel, drawX + scaledW - 2, drawY, 2, scaledH);

        // App Header
        subFont.setColor(Color.ORANGE);
        subFont.draw(batch, "[!] ACADEMIC PORTAL NOTIFICATION", drawX + 25f, drawY + scaledH - 22f);

        subFont.setColor(Color.GRAY);
        subFont.draw(batch, "JUST NOW", drawX + scaledW - 110f, drawY + scaledH - 22f);

        // Divider
        batch.setColor(0.25f, 0.3f, 0.4f, 0.5f);
        batch.draw(whitePixel, drawX + 20f, drawY + scaledH - 48f, scaledW - 40f, 1f);

        // Notification Content emphasizing 2 DAYS and 20 DAYS
        String notifText = "The deadline for project submission has been shifted to [#FF3333]2 DAYS[] from previous deadline of [#FFFF33]20 DAYS[]. Sorry for your inconvenience.";
        chatFont.setColor(new Color(0.95f, 0.95f, 0.98f, 1f));
        chatFont.draw(batch, notifText, drawX + 25f, drawY + scaledH - 75f, scaledW - 50f, -1, true);

        // Dismiss prompt
        if (transitionPhase == TransitionPhase.NONE) {
            float pulse = (float) Math.sin(animTimer * 5f) * 0.3f + 0.7f;
            subFont.setColor(0.6f, 0.8f, 1.0f, pulse);
            layout.setText(subFont, "[ TAP OR PRESS SPACE TO CONTINUE ]");
            subFont.draw(batch, layout, (1280f - layout.width) / 2f, drawY + 28f);
        }
    }

    private void drawGroupChat(boolean isUnseenScene) {
        float chatW = 560f;
        float chatH = 600f;
        float chatX = (1280f - chatW) / 2f;
        float chatY = (720f - chatH) / 2f;

        // Mobile container background
        batch.setColor(0.09f, 0.11f, 0.14f, 0.97f);
        batch.draw(whitePixel, chatX, chatY, chatW, chatH);

        // Outer border
        batch.setColor(0.25f, 0.35f, 0.45f, 0.85f);
        batch.draw(whitePixel, chatX, chatY, chatW, 2);
        batch.draw(whitePixel, chatX, chatY + chatH - 2, chatW, 2);
        batch.draw(whitePixel, chatX, chatY, 2, chatH);
        batch.draw(whitePixel, chatX + chatW - 2, chatY, 2, chatH);

        // Header bar
        batch.setColor(0.14f, 0.19f, 0.26f, 1f);
        batch.draw(whitePixel, chatX, chatY + chatH - 70f, chatW, 70f);

        subFont.setColor(Color.WHITE);
        subFont.getData().setScale(1.2f);
        subFont.draw(batch, "[CHAT] CSE PROJECT GROUP", chatX + 25f, chatY + chatH - 20f);
        subFont.getData().setScale(1.0f);

        subFont.setColor(Color.LIGHT_GRAY);
        subFont.draw(batch, "4 members - Tap for group info", chatX + 25f, chatY + chatH - 45f);

        // Date pill divider
        float pillW = 90f;
        float pillH = 24f;
        float pillX = chatX + (chatW - pillW) / 2f;
        float pillY = chatY + chatH - 110f;
        batch.setColor(0.18f, 0.22f, 0.28f, 0.9f);
        batch.draw(whitePixel, pillX, pillY, pillW, pillH);
        subFont.setColor(Color.GRAY);
        subFont.draw(batch, "TODAY", pillX + 22f, pillY + 17f);

        // Message Bubble entrance animation
        float bubbleProgress = MathUtils.clamp((sceneTimer - 0.1f) / 0.35f, 0f, 1f);
        float bubbleYOffset = (1f - bubbleProgress) * -15f;

        float bubbleW = 440f;
        float bubbleH = 135f;
        float bubbleX = chatX + chatW - bubbleW - 20f;
        float bubbleY = chatY + chatH - 265f + bubbleYOffset;

        // Bubble background (Teal/Green accent for sent message)
        batch.setColor(0.15f, 0.35f, 0.28f, 0.95f * Math.min(1f, bubbleProgress + 0.2f));
        batch.draw(whitePixel, bubbleX, bubbleY, bubbleW, bubbleH);

        // Bubble border
        batch.setColor(0.25f, 0.55f, 0.42f, 0.9f);
        batch.draw(whitePixel, bubbleX, bubbleY, bubbleW, 1);
        batch.draw(whitePixel, bubbleX, bubbleY + bubbleH - 1, bubbleW, 1);
        batch.draw(whitePixel, bubbleX, bubbleY, 1, bubbleH);
        batch.draw(whitePixel, bubbleX + bubbleW - 1, bubbleY, 1, bubbleH);

        // Sender Name
        subFont.setColor(new Color(0.5f, 0.95f, 0.75f, 1f));
        subFont.draw(batch, "You (MC)", bubbleX + 15f, bubbleY + bubbleH - 15f);

        // Message content
        chatFont.setColor(Color.WHITE);
        chatFont.draw(batch, "Guys deadline's been shifted! Start working :(", bubbleX + 15f, bubbleY + bubbleH - 42f, bubbleW - 30f, -1, true);

        // Timestamp & Subtle Integrated Read Status
        subFont.setColor(Color.LIGHT_GRAY);
        subFont.draw(batch, "11:46 PM", bubbleX + 15f, bubbleY + 22f);

        if (!isUnseenScene) {
            // Scene 6: Sent status
            subFont.setColor(Color.CYAN);
            subFont.draw(batch, "Sent", bubbleX + bubbleW - 55f, bubbleY + 22f);
        } else {
            // Scene 9: Integrated subtle read status ("Sent - Seen by 0")
            subFont.setColor(new Color(1.0f, 0.45f, 0.45f, 1f));
            subFont.draw(batch, "Sent - Seen by 0", bubbleX + bubbleW - 145f, bubbleY + 22f);
        }

        // Bottom Chat Input Bar (Simulated UI)
        batch.setColor(0.14f, 0.16f, 0.20f, 1f);
        batch.draw(whitePixel, chatX, chatY, chatW, 55f);

        subFont.setColor(Color.GRAY);
        subFont.draw(batch, "Type a message...", chatX + 25f, chatY + 33f);

        // Prompt
        if (transitionPhase == TransitionPhase.NONE) {
            float pulse = (float) Math.sin(animTimer * 5f) * 0.3f + 0.7f;
            subFont.setColor(0.7f, 0.7f, 0.7f, pulse);
            subFont.draw(batch, "PRESS [SPACE / CLICK] TO CONTINUE >", chatX + chatW - 275f, chatY - 25f);
        }
    }

    private void drawTimePassesScreen() {
        titleFont.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        layout.setText(titleFont, "After some time");
        titleFont.draw(batch, "After some time", (1280f - layout.width) / 2f, (720f + layout.height) / 2f);
    }

    private void drawScreamScene() {
        float wrapWidth = 960f;

        // Text jitter offset
        float textJitterX = MathUtils.random(-3f, 3f);
        float textJitterY = MathUtils.random(-3f, 3f);

        dialogueFont.setColor(Color.YELLOW);
        layout.setText(dialogueFont, "Whattt, lord why?....... Whhhyyyy? AAArRGhhhhhHH", Color.YELLOW, wrapWidth, Align.center, true);

        float drawX = (1280f - wrapWidth) / 2f + textJitterX;
        float drawY = (720f + layout.height) / 2f + textJitterY;

        dialogueFont.draw(batch, layout, drawX, drawY);

        if (transitionPhase == TransitionPhase.NONE) {
            float pulse = (float) Math.sin(animTimer * 8f) * 0.4f + 0.6f;
            subFont.setColor(1.0f, 0.3f, 0.3f, pulse);
            layout.setText(subFont, "PRESS [SPACE / CLICK] TO CONTINUE >");
            subFont.draw(batch, layout, (1280f - layout.width) / 2f, 60f);
        }
    }

    private void drawObjectiveCard(String cardText, Color textColor) {
        titleFont.setColor(textColor.r, textColor.g, textColor.b, 1.0f);
        layout.setText(titleFont, cardText);
        titleFont.draw(batch, cardText, (1280f - layout.width) / 2f, (720f + layout.height) / 2f);

        if (transitionPhase == TransitionPhase.NONE) {
            subFont.setColor(0.5f, 0.5f, 0.5f, 0.7f);
            layout.setText(subFont, "PRESS [SPACE] TO CONTINUE");
            subFont.draw(batch, layout, (1280f - layout.width) / 2f, 60f);
        }
    }

    private void updateTransition(float delta) {
        if (transitionPhase == TransitionPhase.NONE) return;

        transitionTimer += delta;

        switch (transitionPhase) {
            case FADE_OUT:
                if (transitionTimer >= FADE_OUT_DURATION) {
                    transitionPhase = TransitionPhase.HOLD_BLACK;
                    transitionTimer = 0f;
                    // Update state while screen is pure black
                    currentState = nextState;
                    sceneTimer = 0f;

                    // Trigger Scream scene effects if entering Scene 10
                    if (currentState == SceneState.SCENE_10_SCREAM) {
                        shakeIntensity = 12f;
                        flashAlpha = 1.0f;
                    }
                }
                break;

            case HOLD_BLACK:
                if (transitionTimer >= HOLD_BLACK_DURATION) {
                    transitionPhase = TransitionPhase.FADE_IN;
                    transitionTimer = 0f;
                }
                break;

            case FADE_IN:
                if (transitionTimer >= FADE_IN_DURATION) {
                    transitionPhase = TransitionPhase.NONE;
                    transitionTimer = 0f;
                }
                break;
        }
    }

    private float getTransitionFadeAlpha() {
        switch (transitionPhase) {
            case FADE_OUT:
                return MathUtils.clamp(transitionTimer / FADE_OUT_DURATION, 0f, 1f);
            case HOLD_BLACK:
                return 1.0f;
            case FADE_IN:
                return MathUtils.clamp(1.0f - (transitionTimer / FADE_IN_DURATION), 0f, 1f);
            default:
                return 0f;
        }
    }

    private void startFadeIn() {
        transitionPhase = TransitionPhase.FADE_IN;
        transitionTimer = 0f;
    }

    private void triggerTransitionTo(SceneState targetState) {
        if (transitionPhase != TransitionPhase.NONE) return;
        nextState = targetState;
        transitionPhase = TransitionPhase.FADE_OUT;
        transitionTimer = 0f;
    }

    private void drawSkipButton() {
        boolean isHovered = skipButtonBounds.contains(mousePos.x, mousePos.y);

        // Background
        if (isHovered) {
            batch.setColor(0.20f, 0.25f, 0.35f, 0.90f);
        } else {
            batch.setColor(0.10f, 0.12f, 0.16f, 0.75f);
        }
        batch.draw(whitePixel, skipButtonBounds.x, skipButtonBounds.y, skipButtonBounds.width, skipButtonBounds.height);

        // Border Accent
        batch.setColor(isHovered ? Color.WHITE : new Color(0.45f, 0.55f, 0.70f, 0.80f));
        batch.draw(whitePixel, skipButtonBounds.x, skipButtonBounds.y, skipButtonBounds.width, 2);
        batch.draw(whitePixel, skipButtonBounds.x, skipButtonBounds.y + skipButtonBounds.height - 2, skipButtonBounds.width, 2);
        batch.draw(whitePixel, skipButtonBounds.x, skipButtonBounds.y, 2, skipButtonBounds.height);
        batch.draw(whitePixel, skipButtonBounds.x + skipButtonBounds.width - 2, skipButtonBounds.y, 2, skipButtonBounds.height);

        // Text
        subFont.setColor(isHovered ? Color.WHITE : new Color(0.85f, 0.88f, 0.95f, 0.90f));
        layout.setText(subFont, "SKIP");
        subFont.draw(batch, "SKIP", skipButtonBounds.x + (skipButtonBounds.width - layout.width) / 2f,
                skipButtonBounds.y + (skipButtonBounds.height + layout.height) / 2f);
    }

    private void handleInput(float delta) {
        // Skip Button Click Check (Active at all times during intro)
        if (Gdx.input.justTouched() && skipButtonBounds.contains(mousePos.x, mousePos.y)) {
            transitionToTitleScreen();
            return;
        }

        // Auto-advance for Scene 7 ("After some time")
        if (currentState == SceneState.SCENE_7_TIME_PASSES) {
            if (sceneTimer >= 2.2f && transitionPhase == TransitionPhase.NONE) {
                triggerTransitionTo(SceneState.SCENE_8_PART_DONE);
                return;
            }
        }

        // Auto-advance for Objective Cards after ~2.4s hold
        if (currentState == SceneState.CARD_1_2_DAYS ||
            currentState == SceneState.CARD_2_FIND_TEAMMATES ||
            currentState == SceneState.CARD_3_FINISH_PROJECT) {
            if (sceneTimer >= 2.4f && transitionPhase == TransitionPhase.NONE) {
                advanceToNextState();
                return;
            }
        }

        // Block player input while a transition is in progress
        if (transitionPhase != TransitionPhase.NONE) {
            return;
        }

        boolean spacePressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean enterPressed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        boolean mouseClicked = Gdx.input.justTouched();

        if (spacePressed || enterPressed || mouseClicked) {
            advanceToNextState();
        }
    }

    private void advanceToNextState() {
        switch (currentState) {
            case SCENE_1_SLEEPING:
                triggerTransitionTo(SceneState.SCENE_2_WAKING);
                break;
            case SCENE_2_WAKING:
                triggerTransitionTo(SceneState.SCENE_3_NOTIFICATION);
                break;
            case SCENE_3_NOTIFICATION:
                triggerTransitionTo(SceneState.SCENE_4_REACTION);
                break;
            case SCENE_4_REACTION:
                triggerTransitionTo(SceneState.SCENE_5_DECISION);
                break;
            case SCENE_5_DECISION:
                triggerTransitionTo(SceneState.SCENE_6_GROUP_CHAT_SENT);
                break;
            case SCENE_6_GROUP_CHAT_SENT:
                triggerTransitionTo(SceneState.SCENE_7_TIME_PASSES);
                break;
            case SCENE_7_TIME_PASSES:
                triggerTransitionTo(SceneState.SCENE_8_PART_DONE);
                break;
            case SCENE_8_PART_DONE:
                triggerTransitionTo(SceneState.SCENE_9_GROUP_CHAT_UNSEEN);
                break;
            case SCENE_9_GROUP_CHAT_UNSEEN:
                triggerTransitionTo(SceneState.SCENE_10_SCREAM);
                break;
            case SCENE_10_SCREAM:
                triggerTransitionTo(SceneState.CARD_1_2_DAYS);
                break;
            case CARD_1_2_DAYS:
                triggerTransitionTo(SceneState.CARD_2_FIND_TEAMMATES);
                break;
            case CARD_2_FIND_TEAMMATES:
                triggerTransitionTo(SceneState.CARD_3_FINISH_PROJECT);
                break;
            case CARD_3_FINISH_PROJECT:
                transitionToTitleScreen();
                break;
        }
    }

    private void transitionToTitleScreen() {
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
        if (whitePixel != null) whitePixel.dispose();
        if (dialogueFont != null) dialogueFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (chatFont != null) chatFont.dispose();
        if (subFont != null) subFont.dispose();
    }
}
