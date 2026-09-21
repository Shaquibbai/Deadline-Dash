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

import io.github.shaquibbai.deadlinedash.assets.AssetPaths;
import io.github.shaquibbai.deadlinedash.npc.NPC;
import io.github.shaquibbai.deadlinedash.npc.NPCConfig;
import io.github.shaquibbai.deadlinedash.npc.NPCModelResolver;
import io.github.shaquibbai.deadlinedash.npc.StaticNPCVisual;

/**
 * Manages Tiled map loading, rendering, collision extraction, and scene transitions.
 */
public class MapManager {
    private String currentMapPath;
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    private int tileWidth = 64;
    private int tileHeight = 64;

    private final Rectangle worldBounds = new Rectangle();
    private final Vector2 defaultSpawnPosition = new Vector2();
    private final com.badlogic.gdx.utils.Array<com.badlogic.gdx.math.Polygon> collisionPolygons = new com.badlogic.gdx.utils.Array<>();
    private final com.badlogic.gdx.utils.Array<SceneTransition> sceneTransitions = new com.badlogic.gdx.utils.Array<>();
    private final com.badlogic.gdx.utils.Array<NPC> npcs = new com.badlogic.gdx.utils.Array<>();

    private int[] backgroundLayerIndices = new int[0];
    private int[] foregroundLayerIndices = new int[0];

    public MapManager(String mapPath, SpriteBatch batch) {
        loadMap(mapPath, batch);
    }

    public void loadMap(String mapPath, SpriteBatch batch) {
        this.currentMapPath = mapPath;
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
        loadSceneTransitions();
        loadNPCs();
        updateLayerIndices();
    }

    private com.badlogic.gdx.math.Polygon createPolygonFromMapObject(MapObject object) {
        if (object instanceof com.badlogic.gdx.maps.objects.RectangleMapObject rectObject) {
            Rectangle rect = rectObject.getRectangle();
            float rotation = 0f;
            Object rotProp = object.getProperties().get("rotation");
            if (rotProp instanceof Number num) {
                rotation = num.floatValue();
            } else if (rotProp instanceof String str) {
                try {
                    rotation = Float.parseFloat(str);
                } catch (NumberFormatException ignored) {
                }
            }

            // In Tiled, rectangle rotation pivots around the top-left corner.
            // In LibGDX world coordinates (bottom-left origin), the top-left is (rect.x, rect.y + rect.height).
            // Clockwise rotation in Tiled screen coordinates corresponds to negative degrees (-rotation) in LibGDX.
            com.badlogic.gdx.math.Polygon polygon = new com.badlogic.gdx.math.Polygon(new float[] {
                0, 0,
                rect.width, 0,
                rect.width, -rect.height,
                0, -rect.height
            });
            polygon.setPosition(rect.x, rect.y + rect.height);
            polygon.setOrigin(0, 0);
            if (rotation != 0f) {
                polygon.setRotation(-rotation);
            }
            return polygon;
        } else if (object instanceof com.badlogic.gdx.maps.objects.PolygonMapObject polyObject) {
            return polyObject.getPolygon();
        }
        return null;
    }

    private void loadCollisionObjects() {
        collisionPolygons.clear();
        MapLayer collisionLayer = tiledMap.getLayers().get("Collision");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                com.badlogic.gdx.math.Polygon polygon = createPolygonFromMapObject(object);
                if (polygon != null) {
                    collisionPolygons.add(polygon);
                }
            }
        }
    }

    private void loadSceneTransitions() {
        sceneTransitions.clear();
        MapLayer transitionLayer = tiledMap.getLayers().get("SceneTransitions");
        if (transitionLayer != null) {
            for (MapObject object : transitionLayer.getObjects()) {
                com.badlogic.gdx.math.Polygon polygon = createPolygonFromMapObject(object);
                if (polygon != null) {
                    String name = object.getName();
                    String targetMapPath = null;
                    String targetSpawnName = null;

                    // Check custom properties first if defined in map editor
                    if (object.getProperties().containsKey("targetMap")) {
                        targetMapPath = object.getProperties().get("targetMap", String.class);
                    }
                    if (object.getProperties().containsKey("targetSpawn")) {
                        targetSpawnName = object.getProperties().get("targetSpawn", String.class);
                    }

                    // Standard transition registry mapping by identifier
                    if (targetMapPath == null && name != null) {
                        switch (name) {
                            case "AB1_to_AB1Inside":
                                targetMapPath = AssetPaths.MAP_AB1_LOBBY;
                                targetSpawnName = "PlayerSpawn_from_AB1";
                                break;
                            case "AB1_Lobby_to_AB1":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_AB1_Lobby";
                                break;
                            case "AB1_Lobby_to_AB1_Class1":
                                targetMapPath = AssetPaths.MAP_AB1_CLASS1;
                                targetSpawnName = "PlayerSpawn_from_AB1_Lobby";
                                break;
                            case "AB1_Class1_to_AB1_Lobby":
                                targetMapPath = AssetPaths.MAP_AB1_LOBBY;
                                targetSpawnName = "PlayerSpawn_from_AB1_Class1";
                                break;
                            case "Cafe_to_CafeInside":
                                targetMapPath = AssetPaths.MAP_CAFE_INSIDE;
                                targetSpawnName = "PlayerSpawn_from_CafeRight";
                                break;
                            case "Cafe_to_LeftCafeInside":
                                targetMapPath = AssetPaths.MAP_CAFE_INSIDE;
                                targetSpawnName = "PlayerSpawn_from_CafeLeft";
                                break;
                            case "Cafe_to_RightCafeInside":
                                targetMapPath = AssetPaths.MAP_CAFE_INSIDE;
                                targetSpawnName = "PlayerSpawn_from_CafeRight";
                                break;
                            case "CafeInsideLeft_to_CafeLeft":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_CafeInsideLeft";
                                break;
                            case "CafeInsideRight_to_CafeRight":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_CafeInsideRight";
                                break;
                            case "CDSFront_to_CDSInsideFront":
                            case "CDSFront_to_CdsInsideFront":
                                targetMapPath = AssetPaths.MAP_CDS;
                                targetSpawnName = "PlayerSpawn_from_CDSFront";
                                break;
                            case "CDSBack_to_CDSInsideBack":
                            case "CDSBack_to_CdsInsideBack":
                                targetMapPath = AssetPaths.MAP_CDS;
                                targetSpawnName = "PlayerSpawn_from_CDSBack";
                                break;
                            case "CDSInsideFront_to_CDSFront":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_CDSInsideFront";
                                break;
                            case "CDSInsideBack_to_CDSBack":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_CDSInsideBack";
                                break;
                            case "AB2_to_AB2Inside":
                                targetMapPath = AssetPaths.MAP_AB2_LOBBY;
                                targetSpawnName = "PlayerSpawn";
                                break;
                            case "AB2_Lobby_to_AB2":
                                targetMapPath = AssetPaths.MAP_IUT_CAMPUS;
                                targetSpawnName = "PlayerSpawn_from_AB2_Lobby";
                                break;
                            case "CDS_to_CdsInside":
                            default:
                                // CDS and unmapped transitions keep detection behavior only
                                targetMapPath = null;
                                targetSpawnName = null;
                                break;
                        }
                    }

                    sceneTransitions.add(new SceneTransition(name, polygon, targetMapPath, targetSpawnName));
                }
            }
        }
    }

    private void loadNPCs() {
        disposeNPCs();
        if (tiledMap == null) return;

        // Try standard NPC object layers
        MapLayer npcLayer = tiledMap.getLayers().get("NPCs");
        if (npcLayer == null) {
            npcLayer = tiledMap.getLayers().get("NPC");
        }

        if (npcLayer != null) {
            for (MapObject object : npcLayer.getObjects()) {
                parseAndAddNPC(object);
            }
        } else {
            // Check other object layers for objects of type NPC or containing model property
            for (MapLayer layer : tiledMap.getLayers()) {
                if (layer instanceof TiledMapTileLayer) continue;
                for (MapObject object : layer.getObjects()) {
                    if ("NPC".equalsIgnoreCase(object.getName()) ||
                        "NPC".equalsIgnoreCase(object.getProperties().get("type", String.class)) ||
                        object.getProperties().containsKey("model")) {
                        parseAndAddNPC(object);
                    }
                }
            }
        }
    }

    private void parseAndAddNPC(MapObject object) {
        float x = 0f;
        float y = 0f;
        float width = 40f;
        float height = 60f;

        if (object instanceof com.badlogic.gdx.maps.objects.RectangleMapObject rectObject) {
            Rectangle rect = rectObject.getRectangle();
            x = rect.x;
            y = rect.y;
            width = rect.width;
            height = rect.height;
        } else if (object instanceof com.badlogic.gdx.maps.objects.PointMapObject pointObject) {
            x = pointObject.getPoint().x;
            y = pointObject.getPoint().y;
        } else if (object.getProperties().containsKey("x") && object.getProperties().containsKey("y")) {
            Object xObj = object.getProperties().get("x");
            Object yObj = object.getProperties().get("y");
            x = xObj instanceof Number ? ((Number) xObj).floatValue() : Float.parseFloat(xObj.toString());
            y = yObj instanceof Number ? ((Number) yObj).floatValue() : Float.parseFloat(yObj.toString());
            if (object.getProperties().containsKey("width")) {
                Object wObj = object.getProperties().get("width");
                width = wObj instanceof Number ? ((Number) wObj).floatValue() : Float.parseFloat(wObj.toString());
            }
            if (object.getProperties().containsKey("height")) {
                Object hObj = object.getProperties().get("height");
                height = hObj instanceof Number ? ((Number) hObj).floatValue() : Float.parseFloat(hObj.toString());
            }
        }

        String model = object.getProperties().get("model", String.class);
        String name = object.getProperties().get("name", String.class);
        if (name == null || name.trim().isEmpty()) {
            name = object.getName();
        }
        String dialogue = object.getProperties().get("dialogue", "NONE", String.class);
        String quest = object.getProperties().get("quest", "NONE", String.class);

        boolean idleAnimation = false;
        Object idleProp = object.getProperties().get("idleAnimation");
        if (idleProp instanceof Boolean b) idleAnimation = b;
        else if (idleProp instanceof String s) idleAnimation = Boolean.parseBoolean(s);

        boolean interactable = false;
        Object interactProp = object.getProperties().get("interactable");
        if (interactProp instanceof Boolean b) interactable = b;
        else if (interactProp instanceof String s) interactable = Boolean.parseBoolean(s);

        float interactionRange = 0f;
        Object rangeProp = object.getProperties().get("interactionRange");
        if (rangeProp instanceof Number num) interactionRange = num.floatValue();
        else if (rangeProp instanceof String s) {
            try { interactionRange = Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        }

        NPCConfig config = new NPCConfig(name, model, dialogue, quest, idleAnimation, interactable, interactionRange);
        String assetPath = NPCModelResolver.resolveAndVerify(model);
        StaticNPCVisual visual = new StaticNPCVisual(assetPath);
        NPC npc = new NPC(config, x, y, width, height, visual);
        npcs.add(npc);
        System.out.printf("[NPC] Loaded NPC '%s' (model: '%s') at (%.1f, %.1f) in map '%s'%n",
            config.getName(), config.getModel(), x, y, currentMapPath);
    }

    private void disposeNPCs() {
        for (NPC npc : npcs) {
            npc.dispose();
        }
        npcs.clear();
    }

    /**
     * Finds a spawn point in the currently loaded map from the specified layer and object name.
     * Fails gracefully and returns null if layer or spawn point does not exist.
     */
    public Vector2 getSpawnPosition(String layerName, String objectName) {
        if (tiledMap == null) {
            System.err.printf("[MAP] Cannot find spawn: tiledMap is null%n");
            return null;
        }

        String searchLayer = layerName != null ? layerName : "PlayerSpawns";
        String searchTarget = objectName;
        if (searchTarget == null) {
            System.err.printf("[MAP] Cannot find spawn: objectName is null in map '%s'%n", currentMapPath);
            return null;
        }

        MapLayer spawnLayer = tiledMap.getLayers().get(searchLayer);
        if (spawnLayer == null) {
            System.err.printf("[MAP] Spawn layer '%s' not found in map '%s'%n", searchLayer, currentMapPath);
            return null;
        }

        for (MapObject object : spawnLayer.getObjects()) {
            String name = object.getName();
            if (searchTarget.equals(name) ||
                (name != null && name.replaceAll("_", "").equalsIgnoreCase(searchTarget.replaceAll("_", "")))) {
                if (object instanceof com.badlogic.gdx.maps.objects.PointMapObject pointObject) {
                    return new Vector2(pointObject.getPoint().x, pointObject.getPoint().y);
                } else if (object instanceof com.badlogic.gdx.maps.objects.RectangleMapObject rectObject) {
                    Rectangle rect = rectObject.getRectangle();
                    return new Vector2(rect.x, rect.y);
                } else if (object.getProperties().containsKey("x") && object.getProperties().containsKey("y")) {
                    Object xObj = object.getProperties().get("x");
                    Object yObj = object.getProperties().get("y");
                    float x = xObj instanceof Number ? ((Number) xObj).floatValue() : Float.parseFloat(xObj.toString());
                    float y = yObj instanceof Number ? ((Number) yObj).floatValue() : Float.parseFloat(yObj.toString());
                    return new Vector2(x, y);
                }
            }
        }

        System.err.printf("[MAP] Spawn object '%s' not found in layer '%s' of map '%s'%n", searchTarget, searchLayer, currentMapPath);
        return null;
    }

    /**
     * Sets the world bounds dynamically based on map dimensions,
     * and sets default campus entrance spawn position.
     */
    private void calculateWorldBounds() {
        if (tiledMap != null && tiledMap.getProperties().containsKey("width")) {
            int mapWidth = tiledMap.getProperties().get("width", Integer.class);
            int mapHeight = tiledMap.getProperties().get("height", Integer.class);
            tileWidth = tiledMap.getProperties().get("tilewidth", 64, Integer.class);
            tileHeight = tiledMap.getProperties().get("tileheight", 64, Integer.class);

            float totalWidth = mapWidth * tileWidth;
            float totalHeight = mapHeight * tileHeight;
            worldBounds.set(0.0f, 0.0f, totalWidth, totalHeight);
        } else {
            // Fallback 1024x528 campus envelope in 64x64 tiles
            float minX = 0.0f;
            float maxX = 65536.0f;
            float minY = 0.0f;
            float maxY = 33792.0f;
            worldBounds.set(minX, minY, maxX - minX, maxY - minY);
        }

        // Initial spawn coordinate at outdoor campus position (tile X~31, Y~451)
        defaultSpawnPosition.set(2007.8f, 28838.5f);
    }

    private void updateLayerIndices() {
        if (tiledMap == null) {
            backgroundLayerIndices = new int[0];
            foregroundLayerIndices = new int[0];
            return;
        }

        com.badlogic.gdx.utils.IntArray bgList = new com.badlogic.gdx.utils.IntArray();
        com.badlogic.gdx.utils.IntArray fgList = new com.badlogic.gdx.utils.IntArray();

        com.badlogic.gdx.maps.MapLayers layers = tiledMap.getLayers();
        for (int i = 0; i < layers.getCount(); i++) {
            com.badlogic.gdx.maps.MapLayer layer = layers.get(i);
            if ("Foreground".equalsIgnoreCase(layer.getName())) {
                fgList.add(i);
            } else {
                bgList.add(i);
            }
        }

        backgroundLayerIndices = bgList.toArray();
        foregroundLayerIndices = fgList.toArray();
    }

    public void renderBackground(OrthographicCamera camera) {
        if (mapRenderer != null && backgroundLayerIndices != null && backgroundLayerIndices.length > 0) {
            mapRenderer.setView(camera);
            mapRenderer.render(backgroundLayerIndices);
        }
    }

    public void renderForeground(OrthographicCamera camera) {
        if (mapRenderer != null && foregroundLayerIndices != null && foregroundLayerIndices.length > 0) {
            mapRenderer.setView(camera);
            mapRenderer.render(foregroundLayerIndices);
        }
    }

    public void render(OrthographicCamera camera) {
        renderBackground(camera);
        renderForeground(camera);
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public com.badlogic.gdx.utils.Array<com.badlogic.gdx.math.Polygon> getCollisionPolygons() {
        return collisionPolygons;
    }

    public com.badlogic.gdx.utils.Array<SceneTransition> getSceneTransitions() {
        return sceneTransitions;
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

    public String getCurrentMapPath() {
        return currentMapPath;
    }

    public com.badlogic.gdx.utils.Array<NPC> getNpcs() {
        return npcs;
    }

    public void renderNPCs(SpriteBatch batch) {
        for (NPC npc : npcs) {
            npc.render(batch);
        }
    }

    public void dispose() {
        disposeNPCs();
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (tiledMap != null) {
            tiledMap.dispose();
        }
    }
}
