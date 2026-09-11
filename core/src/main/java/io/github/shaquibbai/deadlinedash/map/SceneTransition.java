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

    public SceneTransition(String name, Polygon collisionPolygon) {
        this.name = name != null ? name : "";
        this.collisionPolygon = collisionPolygon;
    }

    public String getName() {
        return name;
    }

    public Polygon getCollisionPolygon() {
        return collisionPolygon;
    }
}
