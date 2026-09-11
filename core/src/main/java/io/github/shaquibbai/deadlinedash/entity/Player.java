package io.github.shaquibbai.deadlinedash.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Main Character (MC) entity.
 * Handles 4-directional movement (WASD / Arrow Keys) and renders
 * explicitly mapped, foot-anchored idle and walking animations.
 */
public class Player {
    public enum FacingDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private final Vector2 position;
    private float width;
    private float height;
    private float moveSpeed;

    private FacingDirection facingDirection = FacingDirection.DOWN;
    private boolean isMoving = false;
    private float stateTime = 0f;

    // Sprite Textures
    private Texture walkDownTexture;
    private Texture walkUpTexture;
    private Texture walkLeftTexture;
    private Texture walkRightTexture;
    private Texture idleTexture;
    private Texture placeholderTexture;

    // Animations
    private Animation<TextureRegion> walkDownAnim;
    private Animation<TextureRegion> walkUpAnim;
    private Animation<TextureRegion> walkLeftAnim;
    private Animation<TextureRegion> walkRightAnim;

    private Animation<TextureRegion> idleDownAnim;
    private Animation<TextureRegion> idleUpAnim;
    private Animation<TextureRegion> idleLeftAnim;
    private Animation<TextureRegion> idleRightAnim;

    // Animation & Uniform Cell World Scale Config
    private static final float WALK_FRAME_DURATION = 0.10f;
    private static final float IDLE_FRAME_DURATION = 0.40f;

    // Standardized 128x183 cell dimensions mapped to world-space units
    private static final float CELL_FRAME_WIDTH = 128f;
    private static final float CELL_FRAME_HEIGHT = 183f;
    private static final float RENDER_CELL_HEIGHT = 79.8f; // Uniform world height for all directions (~5% larger)
    private static final float RENDER_CELL_WIDTH = RENDER_CELL_HEIGHT * (CELL_FRAME_WIDTH / CELL_FRAME_HEIGHT); // ~55.8f
    private static final float FOOT_Y_OFFSET = RENDER_CELL_HEIGHT * (10f / CELL_FRAME_HEIGHT); // Feet baseline anchor

    private final com.badlogic.gdx.math.Rectangle collisionBounds = new com.badlogic.gdx.math.Rectangle();
    private final com.badlogic.gdx.math.Polygon playerPolygon = new com.badlogic.gdx.math.Polygon();

    public Player(float startX, float startY, float width, float height, float moveSpeed) {
        this.position = new Vector2(startX, startY);
        this.width = width;
        this.height = height;
        this.moveSpeed = moveSpeed;

        updatePlayerPolygonVertices();
        initAnimations();
    }

    private void updatePlayerPolygonVertices() {
        playerPolygon.setVertices(new float[] {
            0, 0,
            width, 0,
            width, height,
            0, height
        });
    }

    private void initAnimations() {
        try {
            // 1. Walk Down (assets/player/walk/down.png) -> 8 frames horizontal (128x183)
            walkDownTexture = new Texture(Gdx.files.internal("player/walk/down.png"));
            walkDownTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Array<TextureRegion> walkDownFrames = new Array<>();
            for (int i = 0; i < 8; i++) {
                walkDownFrames.add(new TextureRegion(walkDownTexture, i * 128, 0, 128, 183));
            }
            walkDownAnim = new Animation<>(WALK_FRAME_DURATION, walkDownFrames, Animation.PlayMode.LOOP);

            // 2. Walk Up (assets/player/walk/up.png) -> 8 frames horizontal (128x183)
            walkUpTexture = new Texture(Gdx.files.internal("player/walk/up.png"));
            walkUpTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Array<TextureRegion> walkUpFrames = new Array<>();
            for (int i = 0; i < 8; i++) {
                walkUpFrames.add(new TextureRegion(walkUpTexture, i * 128, 0, 128, 183));
            }
            walkUpAnim = new Animation<>(WALK_FRAME_DURATION, walkUpFrames, Animation.PlayMode.LOOP);

            // 3. Walk Left (assets/player/walk/left.png) -> 8 frames horizontal (128x183)
            walkLeftTexture = new Texture(Gdx.files.internal("player/walk/left.png"));
            walkLeftTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Array<TextureRegion> walkLeftFrames = new Array<>();
            for (int i = 0; i < 8; i++) {
                walkLeftFrames.add(new TextureRegion(walkLeftTexture, i * 128, 0, 128, 183));
            }
            walkLeftAnim = new Animation<>(WALK_FRAME_DURATION, walkLeftFrames, Animation.PlayMode.LOOP);

            // 4. Walk Right (assets/player/walk/right.png) -> 8 frames horizontal (363x520)
            walkRightTexture = new Texture(Gdx.files.internal("player/walk/right.png"));
            walkRightTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Array<TextureRegion> walkRightFrames = new Array<>();
            int rightFrameWidth = walkRightTexture.getWidth() / 8;
            int rightFrameHeight = walkRightTexture.getHeight();
            for (int i = 0; i < 8; i++) {
                walkRightFrames.add(new TextureRegion(walkRightTexture, i * rightFrameWidth, 0, rightFrameWidth, rightFrameHeight));
            }
            walkRightAnim = new Animation<>(WALK_FRAME_DURATION, walkRightFrames, Animation.PlayMode.LOOP);

            // 5. Idle Sheet (assets/player/idle/idle_sheet.png) -> 8 horizontal cells (128x183 each)
            idleTexture = new Texture(Gdx.files.internal("player/idle/idle_sheet.png"));
            idleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Explicit mapping according to specification:
            // cells 0–1 -> DOWN idle
            Array<TextureRegion> idleDownFrames = new Array<>();
            idleDownFrames.add(new TextureRegion(idleTexture, 0 * 128, 0, 128, 183));
            idleDownFrames.add(new TextureRegion(idleTexture, 1 * 128, 0, 128, 183));
            idleDownAnim = new Animation<>(IDLE_FRAME_DURATION, idleDownFrames, Animation.PlayMode.LOOP);

            // cells 2–3 -> UP idle
            Array<TextureRegion> idleUpFrames = new Array<>();
            idleUpFrames.add(new TextureRegion(idleTexture, 2 * 128, 0, 128, 183));
            idleUpFrames.add(new TextureRegion(idleTexture, 3 * 128, 0, 128, 183));
            idleUpAnim = new Animation<>(IDLE_FRAME_DURATION, idleUpFrames, Animation.PlayMode.LOOP);

            // cells 6–7 -> LEFT idle
            Array<TextureRegion> idleLeftFrames = new Array<>();
            idleLeftFrames.add(new TextureRegion(idleTexture, 6 * 128, 0, 128, 183));
            idleLeftFrames.add(new TextureRegion(idleTexture, 7 * 128, 0, 128, 183));
            idleLeftAnim = new Animation<>(IDLE_FRAME_DURATION, idleLeftFrames, Animation.PlayMode.LOOP);

            // cells 4–5 -> RIGHT idle
            Array<TextureRegion> idleRightFrames = new Array<>();
            idleRightFrames.add(new TextureRegion(idleTexture, 4 * 128, 0, 128, 183));
            idleRightFrames.add(new TextureRegion(idleTexture, 5 * 128, 0, 128, 183));
            idleRightAnim = new Animation<>(IDLE_FRAME_DURATION, idleRightFrames, Animation.PlayMode.LOOP);

        } catch (Exception e) {
            System.err.println("[PLAYER] Error loading sprite animations: " + e.getMessage());
            createPlaceholderTexture();
        }
    }

    private void createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.NAVY);
        pixmap.fill();
        pixmap.setColor(Color.GOLD);
        pixmap.drawRectangle(0, 0, 32, 32);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(8, 20, 16, 8);
        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public boolean isColliding(float testX, float testY, Array<com.badlogic.gdx.math.Polygon> collisionPolygons) {
        if (collisionPolygons == null || collisionPolygons.isEmpty()) {
            return false;
        }
        collisionBounds.set(testX, testY, width, height);
        playerPolygon.setPosition(testX, testY);

        for (com.badlogic.gdx.math.Polygon poly : collisionPolygons) {
            if (collisionBounds.overlaps(poly.getBoundingRectangle())) {
                if (com.badlogic.gdx.math.Intersector.overlapConvexPolygons(playerPolygon, poly)) {
                    return true;
                }
            }
        }
        return false;
    }

    public io.github.shaquibbai.deadlinedash.map.SceneTransition getOverlappingTransition(Array<io.github.shaquibbai.deadlinedash.map.SceneTransition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return null;
        }
        collisionBounds.set(position.x, position.y, width, height);
        playerPolygon.setPosition(position.x, position.y);

        for (io.github.shaquibbai.deadlinedash.map.SceneTransition transition : transitions) {
            com.badlogic.gdx.math.Polygon poly = transition.getCollisionPolygon();
            if (poly != null && collisionBounds.overlaps(poly.getBoundingRectangle())) {
                if (com.badlogic.gdx.math.Intersector.overlapConvexPolygons(playerPolygon, poly)) {
                    return transition;
                }
            }
        }
        return null;
    }

    public void update(float delta, boolean freeCamera, Array<com.badlogic.gdx.math.Polygon> collisionPolygons) {
        if (freeCamera) {
            return;
        }

        stateTime += delta;

        float moveX = 0f;
        float moveY = 0f;

        boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (up) moveY += 1f;
        if (down) moveY -= 1f;
        if (left) moveX -= 1f;
        if (right) moveX += 1f;

        // Unambiguous key-to-direction state updates:
        // W / UP    -> UP
        // S / DOWN  -> DOWN
        // A / LEFT  -> LEFT
        // D / RIGHT -> RIGHT
        if (up) {
            facingDirection = FacingDirection.UP;
            isMoving = true;
        } else if (down) {
            facingDirection = FacingDirection.DOWN;
            isMoving = true;
        } else if (left) {
            facingDirection = FacingDirection.LEFT;
            isMoving = true;
        } else if (right) {
            facingDirection = FacingDirection.RIGHT;
            isMoving = true;
        } else if (moveX != 0f || moveY != 0f) {
            isMoving = true;
        } else {
            isMoving = false; // Stopped: retains last facing direction
        }

        // Normalize movement vector for uniform diagonal speed
        if (moveX != 0f && moveY != 0f) {
            moveX *= 0.7071f;
            moveY *= 0.7071f;
        }

        float deltaX = moveX * moveSpeed * delta;
        float deltaY = moveY * moveSpeed * delta;

        // Axis-separated movement to allow wall sliding
        if (deltaX != 0f) {
            float newX = position.x + deltaX;
            if (!isColliding(newX, position.y, collisionPolygons)) {
                position.x = newX;
            }
        }

        if (deltaY != 0f) {
            float newY = position.y + deltaY;
            if (!isColliding(position.x, newY, collisionPolygons)) {
                position.y = newY;
            }
        }
    }

    public void update(float delta, boolean freeCamera) {
        update(delta, freeCamera, null);
    }

    public void update(float delta) {
        update(delta, false, null);
    }

    public String getCurrentDirection() {
        return (isMoving ? "WALK_" : "IDLE_") + facingDirection.name();
    }

    public FacingDirection getFacingDirection() {
        return facingDirection;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public TextureRegion getCurrentFrame() {
        if (isMoving) {
            switch (facingDirection) {
                case UP: return walkUpAnim != null ? walkUpAnim.getKeyFrame(stateTime) : null;
                case DOWN: return walkDownAnim != null ? walkDownAnim.getKeyFrame(stateTime) : null;
                case LEFT: return walkLeftAnim != null ? walkLeftAnim.getKeyFrame(stateTime) : null;
                case RIGHT: return walkRightAnim != null ? walkRightAnim.getKeyFrame(stateTime) : null;
            }
        } else {
            switch (facingDirection) {
                case UP: return idleUpAnim != null ? idleUpAnim.getKeyFrame(stateTime) : null;
                case DOWN: return idleDownAnim != null ? idleDownAnim.getKeyFrame(stateTime) : null;
                case LEFT: return idleLeftAnim != null ? idleLeftAnim.getKeyFrame(stateTime) : null;
                case RIGHT: return idleRightAnim != null ? idleRightAnim.getKeyFrame(stateTime) : null;
            }
        }
        return idleDownAnim != null ? idleDownAnim.getKeyFrame(stateTime) : null;
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = getCurrentFrame();

        if (currentFrame != null) {
            // Uniform render width & height for all directions (128x183 frame canvas)
            float drawWidth = RENDER_CELL_WIDTH;
            float drawHeight = RENDER_CELL_HEIGHT;

            float centerX = position.x + width / 2f;
            float footY = position.y;

            float drawX = centerX - (drawWidth / 2f);
            float drawY = footY - FOOT_Y_OFFSET;

            batch.draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
        } else if (placeholderTexture != null) {
            batch.draw(placeholderTexture, position.x, position.y, width, height);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getCenterX() {
        return position.x + width / 2f;
    }

    public float getCenterY() {
        return position.y + height / 2f;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        updatePlayerPolygonVertices();
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        updatePlayerPolygonVertices();
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }

    public void dispose() {
        if (walkDownTexture != null) walkDownTexture.dispose();
        if (walkUpTexture != null) walkUpTexture.dispose();
        if (walkLeftTexture != null) walkLeftTexture.dispose();
        if (walkRightTexture != null) walkRightTexture.dispose();
        if (idleTexture != null) idleTexture.dispose();
        if (placeholderTexture != null) placeholderTexture.dispose();
    }
}
