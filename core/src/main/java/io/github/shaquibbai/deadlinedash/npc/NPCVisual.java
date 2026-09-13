package io.github.shaquibbai.deadlinedash.npc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Visual rendering abstraction for NPCs.
 * Allows switching between static sprite rendering and future animated visuals seamlessly.
 */
public interface NPCVisual {
    /**
     * Renders the NPC visual at the given entity position and bounding box dimensions.
     *
     * @param batch  the active SpriteBatch (already begun)
     * @param x      entity bottom-left X in world units
     * @param y      entity bottom-left Y in world units (baseline foot position)
     * @param width  entity bounding box width
     * @param height entity bounding box height
     */
    void render(SpriteBatch batch, float x, float y, float width, float height);

    /**
     * Disposes underlying graphics resources.
     */
    void dispose();
}
