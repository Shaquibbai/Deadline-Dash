package io.github.shaquibbai.deadlinedash.npc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NPCTest {

    @Test
    @DisplayName("NPCConfig stores all properties accurately")
    void testNPCConfig() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "NONE", "NONE", false, false, 0f);

        assertEquals("MiluSir", config.getName());
        assertEquals("Male01_Forward", config.getModel());
        assertEquals("NONE", config.getDialogue());
        assertEquals("NONE", config.getQuest());
        assertFalse(config.isIdleAnimation());
        assertFalse(config.isInteractable());
        assertEquals(0f, config.getInteractionRange());
    }

    @Test
    @DisplayName("NPCConfig handles null fallbacks gracefully")
    void testNPCConfigNullFallbacks() {
        NPCConfig config = new NPCConfig(null, null, null, null, true, true, -5f);

        assertEquals("", config.getName());
        assertEquals("", config.getModel());
        assertEquals("NONE", config.getDialogue());
        assertEquals("NONE", config.getQuest());
        assertTrue(config.isIdleAnimation());
        assertTrue(config.isInteractable());
        assertEquals(0f, config.getInteractionRange());
    }

    @Test
    @DisplayName("NPC entity holds coordinates, bounds, and config")
    void testNPCEntity() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "NONE", "NONE", false, false, 0f);
        NPC npc = new NPC(config, 425.8f, 650.2f, 35f, 57f, null);

        assertEquals("MiluSir", npc.getName());
        assertEquals("Male01_Forward", npc.getModel());
        assertEquals(425.8f, npc.getX(), 0.01f);
        assertEquals(650.2f, npc.getY(), 0.01f);
        assertEquals(425.8f + 17.5f, npc.getCenterX(), 0.01f);
        assertEquals(650.2f + 28.5f, npc.getCenterY(), 0.01f);
        assertEquals(35f, npc.getWidth());
        assertEquals(57f, npc.getHeight());
    }
}
