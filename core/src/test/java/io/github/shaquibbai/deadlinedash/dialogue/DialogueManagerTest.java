package io.github.shaquibbai.deadlinedash.dialogue;

import io.github.shaquibbai.deadlinedash.npc.NPC;
import io.github.shaquibbai.deadlinedash.npc.NPCConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DialogueManagerTest {
    private DialogueManager dialogueManager;

    private static final String TEST_JSON = "{\n" +
        "  \"milu_intro\": {\n" +
        "    \"lines\": [\n" +
        "      \"Hello.\",\n" +
        "      \"Have you seen my notebook?\",\n" +
        "      \"I need it before my next class.\"\n" +
        "    ]\n" +
        "  },\n" +
        "  \"guard_intro\": {\n" +
        "    \"lines\": [\n" +
        "      \"You cannot enter this area yet.\"\n" +
        "    ]\n" +
        "  }\n" +
        "}";

    @BeforeEach
    void setUp() {
        dialogueManager = new DialogueManager();
        dialogueManager.loadDialoguesFromString(TEST_JSON);
    }

    @Test
    @DisplayName("Dialogue data loads and returns dialogue by ID")
    void testLoadDialogues() {
        Dialogue milu = dialogueManager.getDialogue("milu_intro");
        assertNotNull(milu);
        assertEquals("milu_intro", milu.getId());
        assertEquals(3, milu.getLineCount());
        assertEquals("Hello.", milu.getLine(0));
        assertEquals("Have you seen my notebook?", milu.getLine(1));
        assertEquals("I need it before my next class.", milu.getLine(2));

        Dialogue guard = dialogueManager.getDialogue("guard_intro");
        assertNotNull(guard);
        assertEquals(1, guard.getLineCount());
        assertEquals("You cannot enter this area yet.", guard.getLine(0));
    }

    @Test
    @DisplayName("Starting dialogue with interactable NPC activates manager and sets first line")
    void testStartDialogueValid() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        assertFalse(dialogueManager.isActive());
        boolean started = dialogueManager.startDialogue(npc);

        assertTrue(started);
        assertTrue(dialogueManager.isActive());
        assertEquals("MiluSir", dialogueManager.getCurrentSpeaker());
        assertEquals("Hello.", dialogueManager.getCurrentLine());
        assertEquals(0, dialogueManager.getCurrentLineIndex());
        assertEquals(3, dialogueManager.getTotalLines());
        assertFalse(dialogueManager.isLastLine());
    }

    @Test
    @DisplayName("Advancing dialogue steps through all lines and closes after the last line")
    void testAdvanceDialogue() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        dialogueManager.startDialogue(npc);

        // Line 0
        assertEquals(0, dialogueManager.getCurrentLineIndex());
        assertEquals("Hello.", dialogueManager.getCurrentLine());
        assertFalse(dialogueManager.isLastLine());

        // Advance -> Line 1
        dialogueManager.advanceDialogue();
        assertTrue(dialogueManager.isActive());
        assertEquals(1, dialogueManager.getCurrentLineIndex());
        assertEquals("Have you seen my notebook?", dialogueManager.getCurrentLine());
        assertFalse(dialogueManager.isLastLine());

        // Advance -> Line 2 (Last line)
        dialogueManager.advanceDialogue();
        assertTrue(dialogueManager.isActive());
        assertEquals(2, dialogueManager.getCurrentLineIndex());
        assertEquals("I need it before my next class.", dialogueManager.getCurrentLine());
        assertTrue(dialogueManager.isLastLine());

        // Advance -> Ends dialogue
        dialogueManager.advanceDialogue();
        assertFalse(dialogueManager.isActive());
        assertEquals("", dialogueManager.getCurrentSpeaker());
        assertEquals("", dialogueManager.getCurrentLine());
    }

    @Test
    @DisplayName("Non-interactable NPC cannot start dialogue")
    void testStartDialogueNotInteractable() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, false, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        boolean started = dialogueManager.startDialogue(npc);
        assertFalse(started);
        assertFalse(dialogueManager.isActive());
    }

    @Test
    @DisplayName("NPC with NONE dialogue ID cannot start dialogue")
    void testStartDialogueNone() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "NONE", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        boolean started = dialogueManager.startDialogue(npc);
        assertFalse(started);
        assertFalse(dialogueManager.isActive());
    }

    @Test
    @DisplayName("NPC with missing/unknown dialogue ID does not crash and does not start")
    void testStartDialogueMissingId() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "non_existent_dialogue", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        boolean started = dialogueManager.startDialogue(npc);
        assertFalse(started);
        assertFalse(dialogueManager.isActive());
    }

    @Test
    @DisplayName("endDialogue cleanly resets active conversational state")
    void testEndDialogue() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        dialogueManager.startDialogue(npc);
        assertTrue(dialogueManager.isActive());

        dialogueManager.endDialogue();
        assertFalse(dialogueManager.isActive());
        assertNull(dialogueManager.getCurrentNpc());
        assertNull(dialogueManager.getCurrentDialogue());
        assertEquals("", dialogueManager.getCurrentSpeaker());
        assertEquals("", dialogueManager.getCurrentLine());
    }
}
