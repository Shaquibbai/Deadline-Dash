package io.github.shaquibbai.deadlinedash.npc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Entity representing a Non-Player Character (NPC) in the game world.
 * Holds configuration, world bounds, and an abstracted visual renderer.
 */
public class NPC {
    private final NPCConfig config;
    private final Vector2 position;
    private float width;
    private float height;
    private NPCVisual visual;
    private boolean interactable;

    public NPC(NPCConfig config, float x, float y, float width, float height, NPCVisual visual) {
        this.config = config != null ? config : new NPCConfig("", "", "NONE", "NONE", false, false, 0f);
        this.position = new Vector2(x, y);
        this.width = width;
        this.height = height;
        this.visual = visual;
        this.interactable = this.config.isInteractable();
    }

    public void render(SpriteBatch batch) {
        if (visual != null) {
            visual.render(batch, position.x, position.y, width, height);
        }
    }

    public void dispose() {
        if (visual != null) {
            visual.dispose();
            visual = null;
        }
    }

    public NPCConfig getConfig() {
        return config;
    }

    public boolean isInteractable() {
        return interactable;
    }

    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }

    public String getName() {
        return config.getName();
    }

    public String getModel() {
        return config.getModel();
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
        return position.x + (width / 2f);
    }

    public float getCenterY() {
        return position.y + (height / 2f);
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public NPCVisual getVisual() {
        return visual;
    }

    public void setVisual(NPCVisual visual) {
        this.visual = visual;
    }

    @Override
    public String toString() {
        return "NPC{" +
            "name='" + getName() + '\'' +
            ", model='" + getModel() + '\'' +
            ", pos=(" + position.x + ", " + position.y + ")" +
            ", size=" + width + "x" + height +
            '}';
    }
}
