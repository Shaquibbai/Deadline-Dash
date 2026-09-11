package io.github.shaquibbai.deadlinedash.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.shaquibbai.deadlinedash.DeadlineDash;
import io.github.shaquibbai.deadlinedash.assets.AssetPaths;
import io.github.shaquibbai.deadlinedash.entity.Player;
import io.github.shaquibbai.deadlinedash.map.MapManager;

/**
 * Main gameplay screen for Phase 1.
 * Coordinates rendering the IUT campus map, updating player movement, and camera tracking.
 */
public class GameScreen implements Screen {
    private final DeadlineDash game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private OrthographicCamera hudCamera;
    private com.badlogic.gdx.graphics.g2d.BitmapFont debugFont;

    private MapManager mapManager;
    private Player player;

    private io.github.shaquibbai.deadlinedash.map.SceneTransition currentInsideTransition = null;
    private String triggeredTransitionName = "";
    private float transitionMessageTimer = 0f;

    // Camera Zoom Config (0.25 <= camera.zoom <= 2.0)
    private static final float MIN_ZOOM = 2.f;
    private static final float MAX_ZOOM = 20.f;
    private static final float ZOOM_STEP = 0.1f;

    // Configurable virtual viewport size (tested during prototype)
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    public GameScreen(DeadlineDash game) {
        this.game = game;
        this.batch = game.getBatch();

        initCameraAndViewport();
        initWorld();
    }

    private void initCameraAndViewport() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        // Default initial zoom setting, clamped between MIN_ZOOM (0.25) and MAX_ZOOM (2.0)
        camera.zoom = 1.0f;
        viewport.apply();

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        debugFont = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        debugFont.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        debugFont.getData().setScale(1.2f);
    }

    private void initWorld() {
        mapManager = new MapManager(AssetPaths.MAP_IUT_CAMPUS, batch);

        Vector2 spawnPos = mapManager.getDefaultSpawnPosition();

        // Player width and height are independently configurable from world tile size (64x64)
        float playerWidth = 40f;
        float playerHeight = 60f;

        // Temporarily increased movement speed for testing (2500 px/s)
        float moveSpeed = 2500f;

        player = new Player(spawnPos.x, spawnPos.y, playerWidth, playerHeight, moveSpeed);

        // Center camera initially on spawn position
        camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
        camera.update();
    }

    @Override
    public void show() {
    }

    private float logTimer = 0f;

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);

        // Camera Zoom Controls (M: Zoom IN, N: Zoom OUT)
        if (com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
            camera.zoom -= ZOOM_STEP;
        }
        if (com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.N)) {
            camera.zoom += ZOOM_STEP;
        }
        camera.zoom = com.badlogic.gdx.math.MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);

        boolean isF3Pressed = com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.F3);

        if (isF3Pressed) {
            // Free Camera Debug Mode: WASD moves camera directly across map
            float camMoveX = 0f;
            float camMoveY = 0f;
            if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP)) camMoveY += 1f;
            if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN)) camMoveY -= 1f;
            if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) camMoveX -= 1f;
            if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) camMoveX += 1f;

            if (camMoveX != 0f && camMoveY != 0f) {
                camMoveX *= 0.7071f;
                camMoveY *= 0.7071f;
            }

            float freeCamSpeed = 3500f;
            camera.position.x += camMoveX * freeCamSpeed * delta;
            camera.position.y += camMoveY * freeCamSpeed * delta;
            player.update(delta, true, mapManager.getCollisionPolygons()); // Don't move player in F3 mode
        } else {
            // Normal Gameplay Mode: WASD moves player, camera follows player
            player.update(delta, false, mapManager.getCollisionPolygons());
            camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
        }

        // 1. Transition detection (cleanly separated from transition actions)
        io.github.shaquibbai.deadlinedash.map.SceneTransition overlappingTransition = player.getOverlappingTransition(mapManager.getSceneTransitions());
        if (overlappingTransition != null && overlappingTransition != currentInsideTransition) {
            // Edge-triggered: player just entered a new transition zone
            handleTransitionTriggered(overlappingTransition);
        }
        currentInsideTransition = overlappingTransition;

        if (transitionMessageTimer > 0f) {
            transitionMessageTimer -= delta;
        }

        camera.update();

        // Terminal Diagnostic logging every 1.0 second
        logTimer += delta;
        if (logTimer >= 1.0f) {
            logTimer = 0f;
            System.out.printf("[DIAGNOSTIC] Player: (%.1f, %.1f) | Cam: (%.1f, %.1f) | Dir: %s | F3_FreeCam: %b | Transition: %s%n",
                player.getX(), player.getY(), camera.position.x, camera.position.y, player.getCurrentDirection(), isF3Pressed,
                currentInsideTransition != null ? currentInsideTransition.getName() : "NONE");
        }

        // Render Tiled map layers
        mapManager.render(camera);

        // Render entities (Player placeholder)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        batch.end();

        // Render On-Screen Debug HUD
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        float margin = 20f;
        float startY = VIRTUAL_HEIGHT - 20f;
        debugFont.draw(batch, String.format("PLAYER POS : X=%.1f, Y=%.1f [Tile X=%.0f, Y=%.0f]", player.getX(), player.getY(), player.getX() / 64f, player.getY() / 64f), margin, startY);
        debugFont.draw(batch, String.format("CAMERA POS : X=%.1f, Y=%.1f | ZOOM: %.2f (M: In, N: Out)", camera.position.x, camera.position.y, camera.zoom), margin, startY - 25f);
        debugFont.draw(batch, String.format("DIRECTION  : %s | SPEED: %.0f px/s", player.getCurrentDirection(), player.getMoveSpeed()), margin, startY - 50f);
        debugFont.draw(batch, String.format("MODE       : %s", isF3Pressed ? "FREE CAMERA MODE (WASD moves camera)" : "NORMAL MODE (WASD moves player)"), margin, startY - 75f);
        debugFont.draw(batch, "HOLD [F3]  : Move camera independently to inspect campus map", margin, startY - 100f);

        String transitionStatus = "NONE";
        if (currentInsideTransition != null) {
            transitionStatus = "INSIDE: " + currentInsideTransition.getName();
        } else if (transitionMessageTimer > 0f) {
            transitionStatus = "TRIGGERED: " + triggeredTransitionName;
        }
        debugFont.draw(batch, String.format("TRANSITION : %s", transitionStatus), margin, startY - 125f);

        if (transitionMessageTimer > 0f || currentInsideTransition != null) {
            String activeName = currentInsideTransition != null ? currentInsideTransition.getName() : triggeredTransitionName;
            debugFont.setColor(com.badlogic.gdx.graphics.Color.RED);
            debugFont.draw(batch, "TRANSITION TRIGGERED: " + activeName, VIRTUAL_WIDTH / 2f - 220f, VIRTUAL_HEIGHT - 40f);
            debugFont.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        }
        batch.end();
    }

    /**
     * 2. Action triggered upon entering a scene transition zone.
     * Decoupled from detection logic to easily integrate actual map transitions in future phases.
     */
    private void handleTransitionTriggered(io.github.shaquibbai.deadlinedash.map.SceneTransition transition) {
        triggeredTransitionName = transition.getName();
        transitionMessageTimer = 3.0f;
        System.out.printf("[TRANSITION] TRANSITION TRIGGERED: %s at player pos (%.1f, %.1f)%n",
            transition.getName(), player.getX(), player.getY());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        hudCamera.setToOrtho(false, width, height);
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
        if (mapManager != null) {
            mapManager.dispose();
        }
        if (player != null) {
            player.dispose();
        }
        if (debugFont != null) {
            debugFont.dispose();
        }
    }
}
