package io.github.shaquibbai.deadlinedash.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Main Character (MC) entity.
 * Handles continuous 4-directional movement (WASD / Arrow Keys).
 * Player dimensions (width, height) are independently configurable from world tile dimensions.
 */
public class Player {
    private final Vector2 position;
    private float width;
    private float height;
    private float moveSpeed;

    private Texture placeholderTexture;
    private final com.badlogic.gdx.math.Rectangle collisionBounds = new com.badlogic.gdx.math.Rectangle();
    private final com.badlogic.gdx.math.Polygon playerPolygon = new com.badlogic.gdx.math.Polygon();

    public Player(float startX, float startY, float width, float height, float moveSpeed) {
        this.position = new Vector2(startX, startY);
        this.width = width;
        this.height = height;
        this.moveSpeed = moveSpeed;

        updatePlayerPolygonVertices();
        createPlaceholderTexture();
    }

    private void updatePlayerPolygonVertices() {
        playerPolygon.setVertices(new float[] {
            0, 0,
            width, 0,
            width, height,
            0, height
        });
    }

    private void createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.NAVY);
        pixmap.fill();
        pixmap.setColor(Color.GOLD);
        pixmap.drawRectangle(0, 0, 32, 32);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(8, 20, 16, 8); // Simple visor / indicator for facing direction visual
        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private String currentDirection = "IDLE";

    public boolean isColliding(float testX, float testY, com.badlogic.gdx.utils.Array<com.badlogic.gdx.math.Polygon> collisionPolygons) {
        if (collisionPolygons == null || collisionPolygons.isEmpty()) {
            return false;
        }
        collisionBounds.set(testX, testY, width, height);
        playerPolygon.setPosition(testX, testY);

        for (com.badlogic.gdx.math.Polygon poly : collisionPolygons) {
            // Fast AABB broadphase check
            if (collisionBounds.overlaps(poly.getBoundingRectangle())) {
                // Precise SAT convex polygon narrowphase check
                if (com.badlogic.gdx.math.Intersector.overlapConvexPolygons(playerPolygon, poly)) {
                    return true;
                }
            }
        }
        return false;
    }

    public io.github.shaquibbai.deadlinedash.map.SceneTransition getOverlappingTransition(com.badlogic.gdx.utils.Array<io.github.shaquibbai.deadlinedash.map.SceneTransition> transitions) {
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

    public void update(float delta, boolean freeCamera, com.badlogic.gdx.utils.Array<com.badlogic.gdx.math.Polygon> collisionPolygons) {
        if (freeCamera) {
            return;
        }

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

        if (up && !left && !right) currentDirection = "UP";
        else if (down && !left && !right) currentDirection = "DOWN";
        else if (left && !up && !down) currentDirection = "LEFT";
        else if (right && !up && !down) currentDirection = "RIGHT";
        else if (moveX != 0f || moveY != 0f) currentDirection = "MOVING";
        else currentDirection = "IDLE";

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
        return currentDirection;
    }

    public void render(SpriteBatch batch) {
        if (placeholderTexture != null) {
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
        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }
    }
}
