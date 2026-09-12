package io.github.shaquibbai.deadlinedash.map;

import com.badlogic.gdx.math.Polygon;

/**
 * Data structure representing a scene transition trigger area.
 * Encapsulates the transition identifier, collision geometry (including rotation),
 * and can be extended with destination map and target spawn data in future phases.
 */
public class SceneTransition {
    private final String name;
    private final Polygon collisionPolygon;
    private final String targetMapPath;
    private final String targetSpawnName;

    public SceneTransition(String name, Polygon collisionPolygon) {
        this(name, collisionPolygon, null, null);
    }

    public SceneTransition(String name, Polygon collisionPolygon, String targetMapPath, String targetSpawnName) {
        this.name = name != null ? name : "";
        this.collisionPolygon = collisionPolygon;
        this.targetMapPath = targetMapPath;
        this.targetSpawnName = targetSpawnName;
    }

    public String getName() {
        return name;
    }

    public Polygon getCollisionPolygon() {
        return collisionPolygon;
    }

    public boolean hasDestination() {
        return targetMapPath != null && !targetMapPath.isEmpty();
    }

    public String getTargetMapPath() {
        return targetMapPath;
    }

    public String getTargetSpawnName() {
        return targetSpawnName;
    }
}
