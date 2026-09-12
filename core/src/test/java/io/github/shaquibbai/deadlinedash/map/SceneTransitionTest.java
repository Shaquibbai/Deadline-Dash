package io.github.shaquibbai.deadlinedash.map;

import com.badlogic.gdx.math.Polygon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SceneTransitionTest {

    @Test
    public void testTransitionWithoutDestination() {
        Polygon polygon = new Polygon(new float[] { 0, 0, 10, 0, 10, 10, 0, 10 });
        SceneTransition transition = new SceneTransition("CDS_to_CdsInside", polygon);

        assertEquals("CDS_to_CdsInside", transition.getName());
        assertEquals(polygon, transition.getCollisionPolygon());
        assertFalse(transition.hasDestination());
        assertNull(transition.getTargetMapPath());
        assertNull(transition.getTargetSpawnName());
    }

    @Test
    public void testTransitionWithDestination() {
        Polygon polygon = new Polygon(new float[] { 0, 0, 10, 0, 10, 10, 0, 10 });
        SceneTransition transition = new SceneTransition("AB1_to_AB1Inside", polygon, "maps/AB1_Lobby.tmx", "PlayerSpawn_from_AB1");

        assertEquals("AB1_to_AB1Inside", transition.getName());
        assertEquals(polygon, transition.getCollisionPolygon());
        assertTrue(transition.hasDestination());
        assertEquals("maps/AB1_Lobby.tmx", transition.getTargetMapPath());
        assertEquals("PlayerSpawn_from_AB1", transition.getTargetSpawnName());
    }
}
