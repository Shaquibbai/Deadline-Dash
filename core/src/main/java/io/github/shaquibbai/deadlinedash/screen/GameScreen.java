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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.shaquibbai.deadlinedash.DeadlineDash;
import io.github.shaquibbai.deadlinedash.assets.AssetPaths;
import io.github.shaquibbai.deadlinedash.dialogue.DialogueManager;
import io.github.shaquibbai.deadlinedash.dialogue.DialogueUI;
import io.github.shaquibbai.deadlinedash.entity.Player;
import io.github.shaquibbai.deadlinedash.inventory.Backpack;
import io.github.shaquibbai.deadlinedash.inventory.BackpackUI;
import io.github.shaquibbai.deadlinedash.inventory.Item;
import io.github.shaquibbai.deadlinedash.inventory.ItemConfirmationDialog;
import io.github.shaquibbai.deadlinedash.map.MapManager;
import io.github.shaquibbai.deadlinedash.npc.NPC;
import io.github.shaquibbai.deadlinedash.quest.QuestManager;
import io.github.shaquibbai.deadlinedash.rep.RepSystem;
import io.github.shaquibbai.deadlinedash.ending.EndingController;
import io.github.shaquibbai.deadlinedash.ending.EndingScreen;
import io.github.shaquibbai.deadlinedash.ending.EndingType;
import io.github.shaquibbai.deadlinedash.timer.TimeManager;

/**
 * Main gameplay screen for Phase 1.
 * Coordinates rendering the IUT campus map, updating player movement, camera tracking,
 * and rendering HUD overlay UI (REP points HUD, fixed Backpack icon, centered Grid inventory panel, centered confirmation dialogs).
 */
public class GameScreen implements Screen {
    private final DeadlineDash game;
    private final SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private OrthographicCamera hudCamera;
    private Viewport hudViewport;

    private BitmapFont debugFont;
    private BitmapFont hudFont;
    private BitmapFont hudTitleFont;

    private MapManager mapManager;
    private Player player;

    private io.github.shaquibbai.deadlinedash.map.SceneTransition currentInsideTransition = null;
    private String triggeredTransitionName = "";
    private float transitionMessageTimer = 0f;

    // Transition Confirmation State
    private io.github.shaquibbai.deadlinedash.map.SceneTransition pendingTransition = null;
    private boolean isTransitionConfirmationActive = false;

    // Dialogue System Components
    private DialogueManager dialogueManager;
    private DialogueUI dialogueUI;

    // Quest System Component
    private QuestManager questManager;
    private io.github.shaquibbai.deadlinedash.quest.Quest2Controller quest2Controller;

    // Ending System Components
    private EndingController endingController;
    private EndingType pendingEnding = null;

    // REP / Progression System Components
    private RepSystem repSystem;

    // Global Time Manager Component
    private TimeManager timeManager;

    // Backpack & Inventory System Components
    private Backpack backpack;
    private BackpackUI backpackUI;
    private ItemConfirmationDialog itemConfirmationDialog;

    private Texture backpackIconTexture;
    private Texture whitePixel;
    private final Rectangle backpackIconBounds = new Rectangle();
    private final Vector3 hudMousePos = new Vector3();

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
        initTimeManager();
        initRepSystem();
        initBackpackSystem();
        initDialogueSystem();
        initQuestSystem();
    }

    private void initTimeManager() {
        timeManager = new TimeManager();
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public float getTime() {
        return timeManager != null ? timeManager.getTime() : 0f;
    }

    private void initDialogueSystem() {
        dialogueManager = new DialogueManager();
        dialogueUI = new DialogueUI();
    }

    private void initQuestSystem() {
        questManager = new QuestManager(AssetPaths.QUEST_1);
        quest2Controller = new io.github.shaquibbai.deadlinedash.quest.Quest2Controller(AssetPaths.QUEST_2);
        endingController = new EndingController();

        questManager.setQuestCompletionListener((questId, total) -> {
            checkEarlyWinEnding();
        });

        quest2Controller.setCompletionListener(() -> {
            questManager.recordTaskCompleted("Quest2");
            checkEarlyWinEnding();
        });

        dialogueManager.setCompletionListener((dialogueId, npc) -> {
            String npcName = npc != null ? npc.getName() : "";
            String toastMsg = questManager.onDialogueCompleted(dialogueId, npcName, backpack, repSystem);
            if (toastMsg != null && !toastMsg.isEmpty()) {
                itemConfirmationDialog.showToast(toastMsg);
            }
            quest2Controller.onDialogueCompleted(dialogueId, npcName, backpack, repSystem, timeManager);

            syncAllMapNpcs();
        });
        syncAllMapNpcs();
    }

    private void syncAllMapNpcs() {
        if (mapManager == null) return;
        for (NPC npc : mapManager.getNpcs()) {
            String name = npc.getName();
            if (quest2Controller != null && quest2Controller.isManagedNpc(name)) {
                npc.setInteractable(quest2Controller.isNpcInteractable(name));
            } else if (questManager != null) {
                questManager.syncNpcInteractability(npc);
            }
        }
    }

    private String getActiveDialogueForNpc(NPC npc) {
        if (npc == null) return "";
        String name = npc.getName();
        if (quest2Controller != null && quest2Controller.isManagedNpc(name)) {
            return quest2Controller.getDialogueForNpc(name, npc.getConfig().getDialogue(), backpack);
        } else if (questManager != null) {
            return questManager.getDialogueForNpc(npc, backpack);
        }
        return npc.getConfig().getDialogue();
    }

    private void initCameraAndViewport() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.zoom = 1.0f;
        viewport.apply();

        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, hudCamera);
        hudViewport.apply();

        debugFont = new BitmapFont();
        debugFont.setColor(Color.YELLOW);
        debugFont.getData().setScale(1.2f);
    }

    private void initWorld() {
        mapManager = new MapManager(AssetPaths.MAP_IUT_CAMPUS, batch);

        Vector2 spawnPos = mapManager.getDefaultSpawnPosition();

        float playerWidth = 40f;
        float playerHeight = 60f;
        float moveSpeed = 2500f;

        player = new Player(spawnPos.x, spawnPos.y, playerWidth, playerHeight, moveSpeed);

        camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
        camera.update();
    }

    private void initRepSystem() {
        repSystem = new RepSystem(); // Default starting REP = 20
    }

    private void initBackpackSystem() {
        backpack = new Backpack();
        backpackUI = new BackpackUI(backpack);
        itemConfirmationDialog = new ItemConfirmationDialog(backpack);

        // White pixel for UI drawing
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        whitePixel = new Texture(px);
        px.dispose();

        // Check if external backpack icon asset exists
        if (Gdx.files.internal(AssetPaths.BACKPACK_ICON).exists()) {
            backpackIconTexture = new Texture(Gdx.files.internal(AssetPaths.BACKPACK_ICON));
        } else if (Gdx.files.internal("backpack_icon.png").exists()) {
            backpackIconTexture = new Texture(Gdx.files.internal("backpack_icon.png"));
        } else if (Gdx.files.internal("backpack.png").exists()) {
            backpackIconTexture = new Texture(Gdx.files.internal("backpack.png"));
        } else {
            // Fallback pixel-art backpack texture (64x64)
            Pixmap iconPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            iconPixmap.setColor(0, 0, 0, 0);
            iconPixmap.fill();

            iconPixmap.setColor(new Color(0.18f, 0.10f, 0.05f, 1.0f));
            iconPixmap.fillRectangle(13, 11, 38, 43);
            iconPixmap.fillRectangle(10, 16, 44, 33);

            iconPixmap.setColor(new Color(0.72f, 0.44f, 0.18f, 1.0f));
            iconPixmap.fillRectangle(15, 13, 34, 39);
            iconPixmap.fillRectangle(12, 18, 40, 29);

            iconPixmap.setColor(new Color(0.50f, 0.26f, 0.10f, 1.0f));
            iconPixmap.fillRectangle(14, 31, 36, 19);

            iconPixmap.setColor(new Color(0.95f, 0.78f, 0.25f, 1.0f));
            iconPixmap.fillRectangle(21, 28, 6, 10);
            iconPixmap.fillRectangle(37, 28, 6, 10);
            iconPixmap.fillRectangle(20, 50, 24, 4);

            iconPixmap.setColor(new Color(0.62f, 0.35f, 0.14f, 1.0f));
            iconPixmap.fillRectangle(18, 15, 28, 14);
            iconPixmap.setColor(new Color(0.95f, 0.78f, 0.25f, 1.0f));
            iconPixmap.fillRectangle(30, 19, 4, 5);

            backpackIconTexture = new Texture(iconPixmap);
            backpackIconTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            iconPixmap.dispose();
        }

        // HUD Fonts
        hudFont = new BitmapFont();
        hudFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        hudFont.getData().setScale(1.0f);

        hudTitleFont = new BitmapFont();
        hudTitleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        hudTitleFont.getData().setScale(1.35f);

        // Define HUD icon bounds (Top Right: 64x64 at X=1170f, Y=575f)
        backpackIconBounds.set(1170f, 575f, 64f, 64f);
    }

    @Override
    public void show() {
    }

    private float logTimer = 0f;

    // =========================================================================
    // TEMPORARY DEBUG ITEM PICKUP TRIGGER (FOR TESTING ONLY)
    // Isolated debug trigger to test confirmation flow with Pen, Notebook, Controller, Money.
    // Press 'E' in-game to cycle through test item pickups.
    // =========================================================================
    private int debugItemIndex = 0;
    private final Item[] testItems = new Item[] {
        new Item("Pen"),
        new Item("Notebook"),
        new Item("Controller"),
        new Item("Money")
    };
    private final int[] testQuantities = new int[] { 10, 5, 3, 500 };

    private void handleDebugItemPickupTrigger() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (!itemConfirmationDialog.isActive() && !backpackUI.isOpen()) {
                Item testItem = testItems[debugItemIndex % testItems.length];
                int qty = testQuantities[debugItemIndex % testQuantities.length];
                debugItemIndex++;
                itemConfirmationDialog.requestStoreItem(testItem, qty);
            }
        }
    }

    // =========================================================================
    // TEMPORARY DEBUG REP TRIGGER (FOR TESTING ONLY)
    // Isolated debug trigger to test REP system incrementing (+5 REP).
    // Press 'R' in-game to add +5 REP.
    // =========================================================================
    private void handleDebugRepTrigger() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            boolean isUiModalActive = itemConfirmationDialog.isActive() || backpackUI.isOpen();
            if (!isUiModalActive) {
                repSystem.addRep(5);
            }
        }
    }

    // =========================================================================
    // TEMPORARY DEBUG FOOTBALL MINIGAME TRIGGER (FOR TESTING ONLY)
    // Press 'T' in-game to launch FootballScreen minigame.
    // =========================================================================
    private void handleFootballMinigameTrigger() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            boolean isUiModalActive = itemConfirmationDialog.isActive() || backpackUI.isOpen() || dialogueManager.isActive() || isTransitionConfirmationActive;
            if (!isUiModalActive) {
                game.setScreen(new io.github.shaquibbai.deadlinedash.minigame.football.FootballScreen(game, this));
            }
        }
    }
    // =========================================================================

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);

        // If an ending was triggered, immediately transition to EndingScreen
        if (pendingEnding != null) {
            EndingType toTrigger = pendingEnding;
            pendingEnding = null;
            triggerEnding(toTrigger);
            return;
        }

        // Update global countdown timer continuously
        if (timeManager != null) {
            timeManager.update(delta);
            if (timeManager.isExpired() && endingController != null && !endingController.isEndingTriggered()) {
                EndingType ending = endingController.checkTimerExpired(questManager != null ? questManager.getCompletedTaskCount() : 0);
                if (ending != null) {
                    triggerEnding(ending);
                    return;
                }
            }
        }

        // Update Dialogue UI animations
        dialogueUI.update(delta);

        // Camera Zoom Controls (M: Zoom IN, N: Zoom OUT)
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            camera.zoom -= ZOOM_STEP;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            camera.zoom += ZOOM_STEP;
        }
        camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);

        boolean isF3Pressed = Gdx.input.isKeyPressed(Input.Keys.F3);
        boolean isUiModalActive = itemConfirmationDialog.isActive() || backpackUI.isOpen() || dialogueManager.isActive() || isTransitionConfirmationActive || (quest2Controller != null && quest2Controller.isMatchPromptActive());

        if (isF3Pressed && !isUiModalActive) {
            // Free Camera Debug Mode: WASD moves camera directly across map
            float camMoveX = 0f;
            float camMoveY = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) camMoveY += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) camMoveY -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) camMoveX -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camMoveX += 1f;

            if (camMoveX != 0f && camMoveY != 0f) {
                camMoveX *= 0.7071f;
                camMoveY *= 0.7071f;
            }

            float freeCamSpeed = 3500f;
            camera.position.x += camMoveX * freeCamSpeed * delta;
            camera.position.y += camMoveY * freeCamSpeed * delta;
            player.update(delta, true, mapManager.getCollisionPolygons());
        } else {
            // Normal Gameplay Mode: WASD moves player (only when UI modals are inactive)
            if (!isUiModalActive) {
                player.update(delta, false, mapManager.getCollisionPolygons());
            } else {
                player.update(0f, false, mapManager.getCollisionPolygons()); // Freeze player while UI open
            }
            camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
        }

        // 1. Transition detection
        io.github.shaquibbai.deadlinedash.map.SceneTransition overlappingTransition = player.getOverlappingTransition(mapManager.getSceneTransitions());
        if (overlappingTransition != null && overlappingTransition != currentInsideTransition) {
            handleTransitionTriggered(overlappingTransition);
        }
        currentInsideTransition = overlappingTransition;

        if (transitionMessageTimer > 0f) {
            transitionMessageTimer -= delta;
        }

        camera.update();

        // Calculate unprojected mouse position in strict HUD viewport coordinates [0..1280, 0..720]
        hudMousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        hudViewport.unproject(hudMousePos);

        boolean justClicked = Gdx.input.justTouched();

        // Handle Football Match Choice YES / NO
        if (quest2Controller != null && quest2Controller.isMatchPromptActive()) {
            boolean yesPressed = Gdx.input.isKeyJustPressed(Input.Keys.Y) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
            boolean noPressed = Gdx.input.isKeyJustPressed(Input.Keys.N);

            if (justClicked) {
                if (DialogueUI.YES_BUTTON_BOUNDS.contains(hudMousePos.x, hudMousePos.y)) {
                    yesPressed = true;
                } else if (DialogueUI.NO_BUTTON_BOUNDS.contains(hudMousePos.x, hudMousePos.y)) {
                    noPressed = true;
                }
            }

            if (yesPressed) {
                quest2Controller.onMatchChoiceYes();
                launchFootballMinigame();
            } else if (noPressed) {
                quest2Controller.onMatchChoiceNo();
            }
        }

        // Handle Transition Confirmation YES / NO Mouse Clicking
        if (isTransitionConfirmationActive) {
            if (justClicked) {
                if (DialogueUI.YES_BUTTON_BOUNDS.contains(hudMousePos.x, hudMousePos.y)) {
                    confirmTransition();
                } else if (DialogueUI.NO_BUTTON_BOUNDS.contains(hudMousePos.x, hudMousePos.y)) {
                    cancelTransitionConfirmation();
                }
            }
        }

        // Process temporary debug item pickup key trigger ('E'), REP debug key trigger ('R'), and Football minigame ('T')
        handleDebugItemPickupTrigger();
        handleDebugRepTrigger();
        handleFootballMinigameTrigger();

        // Process NPC Interaction and Dialogue Progression ('F')
        handleDialogueInput();

        if (pendingEnding != null) {
            EndingType toTrigger = pendingEnding;
            pendingEnding = null;
            triggerEnding(toTrigger);
            return;
        }

        // Process HUD Backpack icon click (toggle backpack UI)
        if (justClicked && backpackIconBounds.contains(hudMousePos.x, hudMousePos.y)) {
            if (!itemConfirmationDialog.isActive() && !dialogueManager.isActive() && !isTransitionConfirmationActive && (quest2Controller == null || !quest2Controller.isMatchPromptActive())) {
                backpackUI.toggle();
            }
        }

        // ESC key: cancel transition confirmation / close active dialogue / close backpack UI / return to Start Menu (TitleScreen)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (quest2Controller != null && quest2Controller.isMatchPromptActive()) {
                quest2Controller.onMatchChoiceNo();
            } else if (isTransitionConfirmationActive) {
                cancelTransitionConfirmation();
            } else if (dialogueManager.isActive()) {
                dialogueManager.endDialogue();
            } else if (backpackUI.isOpen()) {
                backpackUI.setOpen(false);
            } else {
                returnToTitleScreen();
                return;
            }
        }

        // Mouse wheel / arrow key scroll support for Backpack UI
        if (backpackUI.isOpen()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                backpackUI.scroll(-1f);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                backpackUI.scroll(1f);
            }
        }

        // Update inventory components
        itemConfirmationDialog.update(delta, hudMousePos, justClicked);
        backpackUI.update(delta, hudMousePos, justClicked);

        // Terminal Diagnostic logging every 1.0 second
        logTimer += delta;
        if (logTimer >= 1.0f) {
            logTimer = 0f;
            System.out.printf("[DIAGNOSTIC] Player: (%.1f, %.1f) | Cam: (%.1f, %.1f) | Dir: %s | REP: %d | BackpackItems: %d%n",
                player.getX(), player.getY(), camera.position.x, camera.position.y, player.getCurrentDirection(),
                repSystem.getRep(), backpack.getDistinctItemCount());
        }

        // 1. Render Background Tiled map layers (ground, paths, walls, structures)
        mapManager.renderBackground(camera);

        // 2. Render entities (NPCs, Player, & Quest Markers)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        mapManager.renderNPCs(batch);
        player.render(batch);
        renderQuestMarkers(batch);
        batch.end();

        // 3. Render Foreground Tiled map layer (roofs, tree canopies, overhangs)
        mapManager.renderForeground(camera);

        // Render HUD elements (REP Points HUD, Debug Text, Backpack Icon, Backpack UI, Centered Dialogs, Dialogue UI)
        hudCamera.update();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // 1. Render REP Points HUD (Top Left)
        renderRepHUD();

        // 2. On-Screen Debug Info HUD (Left Side, positioned cleanly below REP)
        float margin = 20f;
        float startY = VIRTUAL_HEIGHT - 65f;
        debugFont.draw(batch, String.format("PLAYER POS : X=%.1f, Y=%.1f [Tile X=%.0f, Y=%.0f]", player.getX(), player.getY(), player.getX() / 64f, player.getY() / 64f), margin, startY);
        debugFont.draw(batch, String.format("CAMERA POS : X=%.1f, Y=%.1f | ZOOM: %.2f (M: In, N: Out)", camera.position.x, camera.position.y, camera.zoom), margin, startY - 25f);
        debugFont.draw(batch, String.format("DIRECTION  : %s | SPEED: %.0f px/s", player.getCurrentDirection(), player.getMoveSpeed()), margin, startY - 50f);
        debugFont.draw(batch, String.format("MODE       : %s", isF3Pressed ? "FREE CAMERA MODE (WASD moves camera)" : "NORMAL MODE (WASD moves player)"), margin, startY - 75f);
        debugFont.draw(batch, "PRESS [F]  : Interact with NPC / Advance Dialogue | [E] : Test item pickup | [R] : +5 REP", margin, startY - 100f);

        String transitionStatus = "NONE";
        if (currentInsideTransition != null) {
            transitionStatus = "INSIDE: " + currentInsideTransition.getName();
        } else if (transitionMessageTimer > 0f) {
            transitionStatus = "TRIGGERED: " + triggeredTransitionName;
        }
        debugFont.draw(batch, String.format("TRANSITION : %s", transitionStatus), margin, startY - 125f);

        if (transitionMessageTimer > 0f || currentInsideTransition != null) {
            String activeName = currentInsideTransition != null ? currentInsideTransition.getName() : triggeredTransitionName;
            debugFont.setColor(Color.RED);
            debugFont.draw(batch, "TRANSITION TRIGGERED: " + activeName, VIRTUAL_WIDTH / 2f - 220f, VIRTUAL_HEIGHT - 40f);
            debugFont.setColor(Color.YELLOW);
        }

        // 3. Render Gameplay HUD Backpack Icon and Countdown Timer (Top Right, Clean - Timer positioned ABOVE Backpack Icon)
        renderTimerHUD();
        renderBackpackHUD();

        // 4. Render Centered Backpack UI Panel (if open)
        backpackUI.render(batch, hudTitleFont, hudFont, whitePixel, hudMousePos);

        // 5. Render Centered Item Storage Confirmation Dialog & Toast Notifications
        itemConfirmationDialog.render(batch, hudTitleFont, hudFont, whitePixel, hudMousePos);

        // 6. Render Dialogue UI / Transition Confirmation UI / Football Match Choice UI
        if (quest2Controller != null && quest2Controller.isMatchPromptActive()) {
            dialogueUI.renderConfirmation(batch, hudFont, whitePixel, "Do you want to play the football match now?", hudMousePos);
        } else if (isTransitionConfirmationActive && pendingTransition != null) {
            String promptMsg = getConfirmationText(pendingTransition.getName());
            dialogueUI.renderConfirmation(batch, hudFont, whitePixel, promptMsg, hudMousePos);
        } else if (dialogueManager.isActive()) {
            dialogueUI.render(batch, hudTitleFont, hudFont, whitePixel, dialogueManager);
        }

        batch.setColor(Color.WHITE); // Ensure batch color state is cleanly reset
        batch.end();
    }

    /**
     * Renders top-left gameplay HUD displaying current REP points.
     * Fixed in screen space [0..1280, 0..720].
     * Layout: REP: 20
     */
    private void renderRepHUD() {
        float repX = 25f;
        float repY = VIRTUAL_HEIGHT - 22f; // 698f (Top Left)

        hudTitleFont.setColor(new Color(0.3f, 0.90f, 1.0f, 1.0f)); // Bright cyan accent
        hudTitleFont.draw(batch, "REP: " + repSystem.getRep(), repX, repY);
        hudTitleFont.setColor(Color.WHITE);
    }

    /**
     * Renders top-right gameplay HUD containing solely the Backpack icon.
     * Fixed in screen space [0..1280, 0..720].
     * Clean, no text labels, no background hover highlights, no screen tinting.
     */
    private void renderBackpackHUD() {
        float iconW = 64f;
        float iconH = 64f;
        float iconX = 1170f;
        float iconY = 575f;

        batch.setColor(Color.WHITE);
        batch.draw(backpackIconTexture, iconX, iconY, iconW, iconH);
        batch.setColor(Color.WHITE);
    }

    /**
     * Renders top-right gameplay HUD displaying global countdown timer (HH : MM).
     * Positioned directly ABOVE the existing Backpack icon at its current HUD location (centered at X=1202f, Y=668f).
     * The surrounding box/background is removed completely.
     * Hour and minute numbers are bold; colon remains normal weight.
     */
    private void renderTimerHUD() {
        if (timeManager == null) return;

        String hhStr = String.format("%02d", timeManager.getDisplayHours());
        String colonStr = " : ";
        String mmStr = String.format("%02d", timeManager.getDisplayMinutes());

        GlyphLayout layoutHH = new GlyphLayout(hudTitleFont, hhStr);
        GlyphLayout layoutColon = new GlyphLayout(hudTitleFont, colonStr);
        GlyphLayout layoutMM = new GlyphLayout(hudTitleFont, mmStr);

        float totalW = layoutHH.width + layoutColon.width + layoutMM.width;
        float centerX = 1202f;
        float centerY = 668f;
        float startX = centerX - (totalW / 2f);
        float textY = centerY + (layoutHH.height / 2f);

        float hhX = startX;
        float colonX = hhX + layoutHH.width;
        float mmX = colonX + layoutColon.width;

        Color timerColor = new Color(0.78f, 0.12f, 0.12f, 1.0f); // Dark red

        // 1. Subtle drop shadow for crisp visibility on any background tile without a box
        hudTitleFont.setColor(0f, 0f, 0f, 0.85f);
        drawBoldText(batch, hudTitleFont, hhStr, hhX + 1.5f, textY - 1.5f);
        hudTitleFont.draw(batch, colonStr, colonX + 1.5f, textY - 1.5f);
        drawBoldText(batch, hudTitleFont, mmStr, mmX + 1.5f, textY - 1.5f);

        // 2. Urgent bold timer text (colon remains normal weight)
        hudTitleFont.setColor(timerColor);
        drawBoldText(batch, hudTitleFont, hhStr, hhX, textY);
        hudTitleFont.draw(batch, colonStr, colonX, textY);
        drawBoldText(batch, hudTitleFont, mmStr, mmX, textY);

        hudTitleFont.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

    private void drawBoldText(SpriteBatch batch, BitmapFont font, String text, float x, float y) {
        font.draw(batch, text, x - 1.2f, y);
        font.draw(batch, text, x + 1.2f, y);
        font.draw(batch, text, x, y - 1.2f);
        font.draw(batch, text, x, y + 1.2f);
        font.draw(batch, text, x - 0.8f, y - 0.8f);
        font.draw(batch, text, x + 0.8f, y + 0.8f);
        font.draw(batch, text, x - 0.8f, y + 0.8f);
        font.draw(batch, text, x + 0.8f, y - 0.8f);
        font.draw(batch, text, x, y);
    }

    private void handleTransitionTriggered(io.github.shaquibbai.deadlinedash.map.SceneTransition transition) {
        triggeredTransitionName = transition.getName();
        transitionMessageTimer = 3.0f;
        System.out.printf("[TRANSITION] TRANSITION TRIGGERED: %s at player pos (%.1f, %.1f)%n",
            transition.getName(), player.getX(), player.getY());

        if (transition.hasDestination()) {
            String confirmationText = getConfirmationText(transition.getName());
            if (confirmationText != null) {
                pendingTransition = transition;
                isTransitionConfirmationActive = true;
                System.out.printf("[TRANSITION] Prompting confirmation for transition: %s%n", transition.getName());
            } else {
                performMapTransition(transition);
            }
        }
    }

    private String getConfirmationText(String transitionName) {
        if (transitionName == null) return null;
        switch (transitionName) {
            case "Cafe_to_LeftCafeInside":
            case "Cafe_to_RightCafeInside":
            case "Cafe_to_CafeInside":
                return "Do you want to enter cafeteria?";
            case "AB1_to_AB1Inside":
                return "Do you want to enter Academic Building 1?";
            case "AB1_Lobby_to_AB1":
                return "Do you want to exit Academic Building 1?";
            case "AB1_Lobby_to_AB1_Class1":
                return "Do you want to enter class?";
            case "AB1_Class1_to_AB1_Lobby":
                return "Do you want to exit classroom?";
            case "CafeInsideLeft_to_CafeLeft":
            case "CafeInsideRight_to_CafeRight":
                return "Do you want to exit Cafeteria?";
            case "CDSFront_to_CDSInsideFront":
            case "CDSFront_to_CdsInsideFront":
            case "CDSBack_to_CDSInsideBack":
            case "CDSBack_to_CdsInsideBack":
                return "Do you want to enter CDS?";
            case "CDSInsideFront_to_CDSFront":
            case "CDSInsideBack_to_CDSBack":
                return "Do you want to exit CDS?";
            default:
                return null;
        }
    }

    private void confirmTransition() {
        if (pendingTransition != null && pendingTransition.hasDestination()) {
            io.github.shaquibbai.deadlinedash.map.SceneTransition target = pendingTransition;
            isTransitionConfirmationActive = false;
            pendingTransition = null;
            performMapTransition(target);
        } else {
            cancelTransitionConfirmation();
        }
    }

    private void cancelTransitionConfirmation() {
        isTransitionConfirmationActive = false;
        pendingTransition = null;
        System.out.println("[TRANSITION] Transition confirmation cancelled.");
    }

    private void performMapTransition(io.github.shaquibbai.deadlinedash.map.SceneTransition transition) {
        String targetMap = transition.getTargetMapPath();
        String targetSpawn = transition.getTargetSpawnName();
        System.out.printf("[TRANSITION] Loading destination map: %s with spawn target: %s%n", targetMap, targetSpawn);

        mapManager.loadMap(targetMap, batch);
        syncAllMapNpcs();

        Vector2 spawnPos = mapManager.getSpawnPosition("PlayerSpawns", targetSpawn);
        if (spawnPos != null) {
            player.setPosition(spawnPos.x, spawnPos.y);
            System.out.printf("[TRANSITION] Placed player at spawn '%s': (%.1f, %.1f)%n", targetSpawn, spawnPos.x, spawnPos.y);
        } else {
            System.err.printf("[TRANSITION] ERROR: Spawn '%s' not found in '%s'. Leaving player at (%.1f, %.1f)%n",
                targetSpawn, targetMap, player.getX(), player.getY());
        }

        // Immediately update camera position to follow newly positioned player
        camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
        camera.update();

        currentInsideTransition = player.getOverlappingTransition(mapManager.getSceneTransitions());
        System.out.printf("[TRANSITION] Successfully completed transition to map: %s%n", targetMap);
    }

    private void launchFootballMinigame() {
        game.setScreen(new io.github.shaquibbai.deadlinedash.minigame.football.FootballScreen(game, this, (cseScore, eeeScore) -> {
            String resultDialogueId = quest2Controller.handleMatchResult(cseScore, eeeScore);
            NPC talha = findNpcByName("Talha");
            if (talha != null) {
                dialogueManager.startDialogue(talha, resultDialogueId);
            }
        }));
    }

    private NPC findNpcByName(String name) {
        if (mapManager == null || name == null) return null;
        for (NPC npc : mapManager.getNpcs()) {
            if (name.equalsIgnoreCase(npc.getName())) {
                return npc;
            }
        }
        return null;
    }

    private void handleDialogueInput() {
        boolean fJustPressed = Gdx.input.isKeyJustPressed(Input.Keys.F);
        if (dialogueManager.isActive()) {
            if (fJustPressed) {
                dialogueManager.advanceDialogue();
            }
        } else if (!itemConfirmationDialog.isActive() && !backpackUI.isOpen() && !isTransitionConfirmationActive && (quest2Controller == null || !quest2Controller.isMatchPromptActive())) {
            if (fJustPressed) {
                NPC closestNpc = findClosestInteractableNPC();
                if (closestNpc != null) {
                    String dialogueId = getActiveDialogueForNpc(closestNpc);
                    dialogueManager.startDialogue(closestNpc, dialogueId);
                }
            }
        }
    }

    private NPC findClosestInteractableNPC() {
        NPC closest = null;
        float minDistanceSq = Float.MAX_VALUE;

        float playerCenterX = player.getCenterX();
        float playerCenterY = player.getCenterY();

        syncAllMapNpcs();

        for (NPC npc : mapManager.getNpcs()) {
            if (!npc.isInteractable()) {
                continue;
            }
            String dialogue = getActiveDialogueForNpc(npc);
            if (dialogue == null || dialogue.trim().isEmpty() || "NONE".equalsIgnoreCase(dialogue.trim())) {
                continue;
            }

            float npcCenterX = npc.getCenterX();
            float npcCenterY = npc.getCenterY();

            float dx = playerCenterX - npcCenterX;
            float dy = playerCenterY - npcCenterY;
            float distSq = dx * dx + dy * dy;

            float maxRange = npc.getConfig().getInteractionRange();
            if (distSq <= maxRange * maxRange && distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closest = npc;
            }
        }
        return closest;
    }

    private void renderQuestMarkers(SpriteBatch batch) {
        if (mapManager == null) return;
        GlyphLayout layout = new GlyphLayout();

        for (NPC npc : mapManager.getNpcs()) {
            String name = npc.getName();
            io.github.shaquibbai.deadlinedash.quest.QuestMarker marker = io.github.shaquibbai.deadlinedash.quest.QuestMarker.NONE;
            if (quest2Controller != null && quest2Controller.isManagedNpc(name)) {
                marker = quest2Controller.getMarkerForNpc(name, backpack);
            } else if (questManager != null) {
                marker = questManager.getMarkerForNpc(npc, backpack);
            }

            if (marker == io.github.shaquibbai.deadlinedash.quest.QuestMarker.NONE) {
                continue;
            }

            String symbol = marker == io.github.shaquibbai.deadlinedash.quest.QuestMarker.NEW_INTERACTION ? "!" : "?";
            Color markerColor = marker == io.github.shaquibbai.deadlinedash.quest.QuestMarker.NEW_INTERACTION
                ? new Color(1.0f, 0.88f, 0.15f, 1.0f) // Bright Yellow
                : new Color(1.0f, 0.25f, 0.25f, 1.0f); // Bright Red

            float centerX = npc.getCenterX();
            float topY = npc.getY() + npc.getHeight() + 20f;

            debugFont.getData().setScale(4.5f);
            debugFont.setColor(markerColor);
            layout.setText(debugFont, symbol);
            debugFont.draw(batch, symbol, centerX - (layout.width / 2f), topY + layout.height);
            debugFont.getData().setScale(1.2f); // Reset scale
        }
        batch.setColor(Color.WHITE);
    }

    public EndingController getEndingController() {
        return endingController;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public io.github.shaquibbai.deadlinedash.quest.Quest2Controller getQuest2Controller() {
        return quest2Controller;
    }

    private void checkEarlyWinEnding() {
        if (endingController == null || questManager == null || timeManager == null) return;
        EndingType ending = endingController.checkEarlyWin(
            questManager.getCompletedTaskCount(),
            timeManager.getRemainingHours()
        );
        if (ending != null) {
            pendingEnding = ending;
        }
    }

    private void triggerEnding(EndingType endingType) {
        game.setScreen(new EndingScreen(game, endingType));
        dispose();
    }

    private void returnToTitleScreen() {
        game.setScreen(new TitleScreen(game));
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        hudViewport.update(width, height, true);
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
        if (mapManager != null) mapManager.dispose();
        if (player != null) player.dispose();
        if (debugFont != null) debugFont.dispose();
        if (hudFont != null) hudFont.dispose();
        if (hudTitleFont != null) hudTitleFont.dispose();
        if (backpackIconTexture != null) backpackIconTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (dialogueUI != null) dialogueUI.dispose();
        if (timeManager != null) timeManager.dispose();
    }
}
