package io.github.shaquibbai.deadlinedash.npc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NPCModelResolverTest {

    @Test
    @DisplayName("Resolves Male model IDs correctly")
    void testResolveMaleModels() {
        assertEquals("characters/npc/male/Male01_Forward.png", NPCModelResolver.resolvePath("Male01_Forward"));
        assertEquals("characters/npc/male/Male01_Left.png", NPCModelResolver.resolvePath("Male01_Left"));
        assertEquals("characters/npc/male/Male01_Backward.png", NPCModelResolver.resolvePath("Male01_Backward"));
        assertEquals("characters/npc/male/Male01_Right.png", NPCModelResolver.resolvePath("Male01_Right"));
    }

    @Test
    @DisplayName("Resolves Female model IDs correctly")
    void testResolveFemaleModels() {
        assertEquals("characters/npc/female/Female01_Forward.png", NPCModelResolver.resolvePath("Female01_Forward"));
        assertEquals("characters/npc/female/Female01_Left.png", NPCModelResolver.resolvePath("Female01_Left"));
    }

    @Test
    @DisplayName("Case-insensitive prefix resolution works")
    void testCaseInsensitivity() {
        assertEquals("characters/npc/male/male02_forward.png", NPCModelResolver.resolvePath("male02_forward"));
        assertEquals("characters/npc/female/FEMALE02_FORWARD.png", NPCModelResolver.resolvePath("FEMALE02_FORWARD"));
    }

    @Test
    @DisplayName("Handles model IDs with .png suffix already present")
    void testHandlesPngSuffix() {
        assertEquals("characters/npc/male/Male01_Forward.png", NPCModelResolver.resolvePath("Male01_Forward.png"));
    }

    @Test
    @DisplayName("Returns null and logs warning for invalid or unrecognized model IDs")
    void testInvalidModelIds() {
        assertNull(NPCModelResolver.resolvePath(null));
        assertNull(NPCModelResolver.resolvePath(""));
        assertNull(NPCModelResolver.resolvePath("   "));
        assertNull(NPCModelResolver.resolvePath("Alien01_Forward"));
        assertNull(NPCModelResolver.resolvePath("MiluSir"));
    }
}
