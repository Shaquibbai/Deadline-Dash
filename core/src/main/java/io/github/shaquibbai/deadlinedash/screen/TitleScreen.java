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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.shaquibbai.deadlinedash.DeadlineDash;
import io.github.shaquibbai.deadlinedash.assets.AssetPaths;

/**
 * Title Screen for Deadline Dash.
 * Displays title artwork with centered menu buttons (START, OPTIONS, QUIT)
 * positioned downward to leave the title logo clear and unobstructed.
 */
public class TitleScreen implements Screen {
    private final DeadlineDash game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture titleTexture;
    private Texture whitePixel;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont subFont;

    private final Vector3 mousePos = new Vector3();
    private boolean isOptionsOpen = false;

    // Centered menu button bounds positioned downward (Viewport: 1280 x 720)
    private static final float BTN_W = 260f;
    private static final float BTN_H = 50f;
    private static final float BTN_X = (1280f - BTN_W) / 2f; // 510f

    private final Rectangle startButton = new Rectangle(BTN_X, 185f, BTN_W, BTN_H);
    private final Rectangle optionsButton = new Rectangle(BTN_X, 122f, BTN_W, BTN_H);
    private final Rectangle quitButton = new Rectangle(BTN_X, 59f, BTN_W, BTN_H);

    // Simple Options modal Back button
    private final Rectangle optionsBackBtn = new Rectangle((1280f - 180f) / 2f, 270f, 180f, 45f);

    private float animTime = 0f;
    private final GlyphLayout layout = new GlyphLayout();
    private float clickCooldown = 0f;

    public TitleScreen(DeadlineDash game) {
        this.game = game;
        this.batch = game.getBatch();

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
        titleTexture = new Texture(Gdx.files.internal(AssetPaths.TITLE_SCREEN_IMAGE));
        titleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        titleFont = new BitmapFont();
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        titleFont.getData().setScale(2.0f);

        buttonFont = new BitmapFont();
        buttonFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        buttonFont.getData().setScale(1.35f);

        subFont = new BitmapFont();
        subFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        subFont.getData().setScale(0.95f);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        animTime += delta;
        ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f);

        // Calculate unprojected mouse position
        mousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Draw Title Background Image with Cover Aspect Ratio
        drawBackground();

        // 2. Draw Main Menu Buttons (positioned downward)
        if (!isOptionsOpen) {
            drawCenterPanel();
            drawMainMenuButtons();
        } else {
            drawOptionsModal();
        }

        batch.end();

        // Handle Input
        handleInput();
    }

    private void drawBackground() {
        float texW = titleTexture.getWidth();
        float texH = titleTexture.getHeight();
        float viewW = viewport.getWorldWidth();
        float viewH = viewport.getWorldHeight();

        float scale = Math.max(viewW / texW, viewH / texH);
        float drawW = texW * scale;
        float drawH = texH * scale;
        float drawX = (viewW - drawW) / 2f;
        float drawY = (viewH - drawH) / 2f;

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(titleTexture, drawX, drawY, drawW, drawH);
    }

    private void drawCenterPanel() {
        // Subtle, lightweight translucent backing panel behind lower menu buttons
        float panelW = 310f;
        float panelH = 196f;
        float panelX = (1280f - panelW) / 2f;
        float panelY = 48f;

        batch.setColor(0f, 0f, 0f, 0.45f);
        batch.draw(whitePixel, panelX, panelY, panelW, panelH);

        // Subtle border accent
        batch.setColor(0.85f, 0.2f, 0.2f, 0.6f);
        batch.draw(whitePixel, panelX, panelY, panelW, 2);
        batch.draw(whitePixel, panelX, panelY + panelH - 2, panelW, 2);
        batch.draw(whitePixel, panelX, panelY, 2, panelH);
        batch.draw(whitePixel, panelX + panelW - 2, panelY, 2, panelH);
    }

    private void drawMainMenuButtons() {
        boolean startHover = startButton.contains(mousePos.x, mousePos.y);
        boolean optionsHover = optionsButton.contains(mousePos.x, mousePos.y);
        boolean quitHover = quitButton.contains(mousePos.x, mousePos.y);

        drawButton(startButton, "START", startHover, new Color(0.88f, 0.2f, 0.2f, 1f));
        drawButton(optionsButton, "OPTIONS", optionsHover, new Color(0.2f, 0.65f, 0.88f, 1f));
        drawButton(quitButton, "QUIT", quitHover, new Color(0.65f, 0.65f, 0.65f, 1f));
    }

    private void drawButton(Rectangle rect, String text, boolean isHovered, Color accentColor) {
        float pulse = isHovered ? (float) Math.sin(animTime * 8f) * 0.04f : 0f;
        float scale = isHovered ? 1.03f + pulse : 1.0f;

        float width = rect.width * scale;
        float height = rect.height * scale;
        float x = rect.x - (width - rect.width) / 2f;
        float y = rect.y - (height - rect.height) / 2f;

        // Button background
        if (isHovered) {
            batch.setColor(accentColor.r, accentColor.g, accentColor.b, 0.90f);
        } else {
            batch.setColor(0.10f, 0.12f, 0.16f, 0.88f);
        }
        batch.draw(whitePixel, x, y, width, height);

        // Border
        batch.setColor(isHovered ? Color.WHITE : accentColor);
        batch.draw(whitePixel, x, y, width, 2);
        batch.draw(whitePixel, x, y + height - 2, width, 2);
        batch.draw(whitePixel, x, y, 2, height);
        batch.draw(whitePixel, x + width - 2, y, 2, height);

        // Text
        buttonFont.setColor(isHovered ? Color.WHITE : new Color(0.92f, 0.92f, 0.96f, 1f));
        layout.setText(buttonFont, text);
        buttonFont.draw(batch, text, x + (width - layout.width) / 2f, y + (height + layout.height) / 2f);
    }

    private void drawOptionsModal() {
        // Dark backdrop overlay
        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(whitePixel, 0, 0, 1280, 720);

        // Simple Options modal panel
        float modalW = 460f;
        float modalH = 220f;
        float modalX = (1280f - modalW) / 2f;
        float modalY = (720f - modalH) / 2f;

        batch.setColor(0.10f, 0.12f, 0.16f, 0.96f);
        batch.draw(whitePixel, modalX, modalY, modalW, modalH);

        // Modal border
        batch.setColor(0.35f, 0.55f, 0.85f, 0.9f);
        batch.draw(whitePixel, modalX, modalY, modalW, 3);
        batch.draw(whitePixel, modalX, modalY + modalH - 3, modalW, 3);
        batch.draw(whitePixel, modalX, modalY, 3, modalH);
        batch.draw(whitePixel, modalX + modalW - 3, modalY, 3, modalH);

        // Message: "Maybe later..."
        buttonFont.setColor(Color.WHITE);
        layout.setText(buttonFont, "Maybe later...");
        buttonFont.draw(batch, "Maybe later...", modalX + (modalW - layout.width) / 2f, modalY + modalH - 60f);

        // Back Button
        boolean backHover = optionsBackBtn.contains(mousePos.x, mousePos.y);
        drawButton(optionsBackBtn, "BACK", backHover, Color.GRAY);
    }

    private void handleInput() {
        if (clickCooldown > 0f) {
            clickCooldown -= Gdx.graphics.getDeltaTime();
            return;
        }

        boolean justClicked = Gdx.input.justTouched();
        boolean spacePressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean enterPressed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        boolean escPressed = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);

        if (!isOptionsOpen) {
            if (spacePressed || enterPressed) {
                startGameplay();
                return;
            }

            if (justClicked) {
                if (startButton.contains(mousePos.x, mousePos.y)) {
                    startGameplay();
                } else if (optionsButton.contains(mousePos.x, mousePos.y)) {
                    isOptionsOpen = true;
                    clickCooldown = 0.2f;
                } else if (quitButton.contains(mousePos.x, mousePos.y)) {
                    Gdx.app.exit();
                }
            }
        } else {
            if (escPressed || spacePressed || enterPressed) {
                isOptionsOpen = false;
                clickCooldown = 0.2f;
                return;
            }

            if (justClicked) {
                if (optionsBackBtn.contains(mousePos.x, mousePos.y)) {
                    isOptionsOpen = false;
                    clickCooldown = 0.2f;
                }
            }
        }
    }

    private void startGameplay() {
        game.setScreen(new GameScreen(game));
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
        if (titleTexture != null) titleTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (titleFont != null) titleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (subFont != null) subFont.dispose();
    }
}
