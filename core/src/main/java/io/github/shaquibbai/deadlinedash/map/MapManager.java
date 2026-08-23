package io.github.shaquibbai.deadlinedash.map;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Manages Tiled map loading, rendering, and infinite map bounds calculation.
 */
public class MapManager {
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    private int tileWidth = 64;
    private int tileHeight = 64;

    private final Rectangle worldBounds = new Rectangle();
    private final Vector2 defaultSpawnPosition = new Vector2();
    private final com.badlogic.gdx.utils.Array<Rectangle> collisionRectangles = new com.badlogic.gdx.utils.Array<>();

    public MapManager(String mapPath, SpriteBatch batch) {
        loadMap(mapPath, batch);
    }

    public void loadMap(String mapPath, SpriteBatch batch) {
        if (tiledMap != null) {
            tiledMap.dispose();
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }

        TmxMapLoader loader = new TmxMapLoader();
        tiledMap = loader.load(mapPath);
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, batch) {
            @Override
            public void renderObject(MapObject object) {
                if (object instanceof TiledMapTileMapObject tileObject) {
                    TiledMapTile tile = tileObject.getTile();
                    if (tile != null) {
                        TextureRegion region = tile.getTextureRegion();
                        if (region != null) {
                            float x = tileObject.getX();
                            float y = tileObject.getY();
                            float width = region.getRegionWidth();
                            float height = region.getRegionHeight();

                            float scaleX = tileObject.getScaleX() * (tileObject.isFlipHorizontally() ? -1f : 1f);
                            float scaleY = tileObject.getScaleY() * (tileObject.isFlipVertically() ? -1f : 1f);
                            float rotation = tileObject.getRotation();

                            getBatch().draw(region, x, y, 0f, 0f, width, height, scaleX, scaleY, rotation);
                        }
                    }
                }
            }
        };

        calculateWorldBounds();
        loadCollisionObjects();
    }

    private void loadCollisionObjects() {
        collisionRectangles.clear();
        MapLayer collisionLayer = tiledMap.getLayers().get("Collision");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof com.badlogic.gdx.maps.objects.RectangleMapObject rectObject) {
                    collisionRectangles.add(rectObject.getRectangle());
                }
            }
        }
    }

    /**
     * Sets the world bounds to the full 1024x528 tile campus envelope
     * (including intentional empty spaces for future building lots),
     * and sets default spawn position at the northern campus entrance road hub (5120, 32000).
     */
    private void calculateWorldBounds() {
        // Full 1024x528 campus envelope in 64x64 tiles
        float minX = 0.0f;
        float maxX = 65536.0f; // 1024 tiles * 64px
        float minY = 0.0f;
        float maxY = 33792.0f; // 528 tiles * 64px

        worldBounds.set(minX, minY, maxX - minX, maxY - minY);

        // Initial spawn coordinate at campus entrance road hub
        defaultSpawnPosition.set(5120.0f, 32000.0f);
    }

    public void render(OrthographicCamera camera) {
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public com.badlogic.gdx.utils.Array<Rectangle> getCollisionRectangles() {
        return collisionRectangles;
    }

    public Rectangle getWorldBounds() {
        return worldBounds;
    }

    public Vector2 getDefaultSpawnPosition() {
        return defaultSpawnPosition;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void dispose() {
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (tiledMap != null) {
            tiledMap.dispose();
        }
    }
}
