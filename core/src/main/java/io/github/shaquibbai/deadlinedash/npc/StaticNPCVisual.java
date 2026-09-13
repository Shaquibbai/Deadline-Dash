package io.github.shaquibbai.deadlinedash.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Static single-image visual representation of an NPC.
 * Adheres to Player world-space scaling and foot-anchoring conventions.
 */
public class StaticNPCVisual implements NPCVisual {
    // Sizing constants aligned with Player world-space conventions
    public static final float RENDER_CELL_HEIGHT = 79.8f;
    public static final float FOOT_Y_OFFSET = RENDER_CELL_HEIGHT * (10f / 183f);

    private Texture texture;
    private float renderWidth;
    private float renderHeight;

    public StaticNPCVisual(String assetPath) {
        if (assetPath != null) {
            try {
                if (Gdx.files != null && Gdx.files.internal(assetPath).exists()) {
                    texture = new Texture(Gdx.files.internal(assetPath));
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    renderHeight = RENDER_CELL_HEIGHT;
                    renderWidth = renderHeight * (texture.getWidth() / (float) texture.getHeight());
                } else {
                    System.err.printf("[NPC] WARNING: Asset '%s' not found for StaticNPCVisual.%n", assetPath);
                    createPlaceholderTexture();
                }
            } catch (Exception e) {
                System.err.printf("[NPC] ERROR: Failed loading NPC texture '%s': %s%n", assetPath, e.getMessage());
                createPlaceholderTexture();
            }
        } else {
            createPlaceholderTexture();
        }
    }

    private void createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(32, 48, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.ORANGE);
        pixmap.fill();
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(0, 0, 32, 48);
        texture = new Texture(pixmap);
        pixmap.dispose();

        renderHeight = RENDER_CELL_HEIGHT;
        renderWidth = renderHeight * (32f / 48f);
    }

    @Override
    public void render(SpriteBatch batch, float x, float y, float width, float height) {
        if (texture == null) {
            return;
        }

        float centerX = x + (width / 2f);
        float footY = y;

        float drawX = centerX - (renderWidth / 2f);
        float drawY = footY - FOOT_Y_OFFSET;

        batch.draw(texture, drawX, drawY, renderWidth, renderHeight);
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    public Texture getTexture() {
        return texture;
    }

    public float getRenderWidth() {
        return renderWidth;
    }

    public float getRenderHeight() {
        return renderHeight;
    }
}
