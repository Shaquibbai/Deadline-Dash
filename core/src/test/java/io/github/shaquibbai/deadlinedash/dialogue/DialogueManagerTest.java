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
        "      {\n" +
        "        \"speaker\": \"npc\",\n" +
        "        \"text\": \"Hello.\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"player\",\n" +
        "        \"text\": \"Hi.\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"npc\",\n" +
        "        \"text\": \"Have you seen my notebook?\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"player\",\n" +
        "        \"text\": \"Yes sir.\"\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"consecutive_test\": {\n" +
        "    \"lines\": [\n" +
        "      {\n" +
        "        \"speaker\": \"npc\",\n" +
        "        \"text\": \"Line 1 from NPC.\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"npc\",\n" +
        "        \"text\": \"Line 2 from NPC.\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"player\",\n" +
        "        \"text\": \"Line 3 from Player.\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"speaker\": \"player\",\n" +
        "        \"text\": \"Line 4 from Player.\"\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"guard_intro\": {\n" +
        "    \"lines\": [\n" +
        "      {\n" +
        "        \"speaker\": \"npc\",\n" +
        "        \"text\": \"You cannot enter this area yet.\"\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}";

    @BeforeEach
    void setUp() {
        dialogueManager = new DialogueManager();
        dialogueManager.loadDialoguesFromString(TEST_JSON);
    }

    @Test
    @DisplayName("Dialogue data loads and returns dialogue by ID with speaker info")
    void testLoadDialogues() {
        Dialogue milu = dialogueManager.getDialogue("milu_intro");
        assertNotNull(milu);
        assertEquals("milu_intro", milu.getId());
        assertEquals(4, milu.getLineCount());

        assertEquals(DialogueSpeaker.NPC, milu.getSpeaker(0));
        assertEquals("Hello.", milu.getText(0));

        assertEquals(DialogueSpeaker.PLAYER, milu.getSpeaker(1));
        assertEquals("Hi.", milu.getText(1));

        assertEquals(DialogueSpeaker.NPC, milu.getSpeaker(2));
        assertEquals("Have you seen my notebook?", milu.getText(2));

        assertEquals(DialogueSpeaker.PLAYER, milu.getSpeaker(3));
        assertEquals("Yes sir.", milu.getText(3));
    }

    @Test
    @DisplayName("Starting dialogue with interactable NPC activates manager and displays NPC name for NPC line")
    void testStartDialogueValid() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        assertFalse(dialogueManager.isActive());
        boolean started = dialogueManager.startDialogue(npc);

        assertTrue(started);
        assertTrue(dialogueManager.isActive());
        assertEquals("MiluSir", dialogueManager.getCurrentSpeaker());
        assertEquals(DialogueSpeaker.NPC, dialogueManager.getCurrentSpeakerType());
        assertEquals("Hello.", dialogueManager.getCurrentLine());
        assertEquals(0, dialogueManager.getCurrentLineIndex());
        assertEquals(4, dialogueManager.getTotalLines());
        assertFalse(dialogueManager.isLastLine());
    }

    @Test
    @DisplayName("Advancing dialogue steps through alternating NPC and player lines in exact order")
    void testAdvanceDialogueAlternating() {
        NPCConfig config = new NPCConfig("MiluSir", "Male01_Forward", "milu_intro", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        dialogueManager.startDialogue(npc);

        // Line 0: NPC (MiluSir)
        assertEquals(0, dialogueManager.getCurrentLineIndex());
        assertEquals("MiluSir", dialogueManager.getCurrentSpeaker());
        assertEquals(DialogueSpeaker.NPC, dialogueManager.getCurrentSpeakerType());
        assertEquals("Hello.", dialogueManager.getCurrentLine());
        assertFalse(dialogueManager.isLastLine());

        // Advance -> Line 1: Player ("Player")
        dialogueManager.advanceDialogue();
        assertTrue(dialogueManager.isActive());
        assertEquals(1, dialogueManager.getCurrentLineIndex());
        assertEquals("Player", dialogueManager.getCurrentSpeaker());
        assertEquals(DialogueSpeaker.PLAYER, dialogueManager.getCurrentSpeakerType());
        assertEquals("Hi.", dialogueManager.getCurrentLine());
        assertFalse(dialogueManager.isLastLine());

        // Advance -> Line 2: NPC (MiluSir)
        dialogueManager.advanceDialogue();
        assertTrue(dialogueManager.isActive());
        assertEquals(2, dialogueManager.getCurrentLineIndex());
        assertEquals("MiluSir", dialogueManager.getCurrentSpeaker());
        assertEquals(DialogueSpeaker.NPC, dialogueManager.getCurrentSpeakerType());
        assertEquals("Have you seen my notebook?", dialogueManager.getCurrentLine());
        assertFalse(dialogueManager.isLastLine());

        // Advance -> Line 3: Player ("Player" - Last line)
        dialogueManager.advanceDialogue();
        assertTrue(dialogueManager.isActive());
        assertEquals(3, dialogueManager.getCurrentLineIndex());
        assertEquals("Player", dialogueManager.getCurrentSpeaker());
        assertEquals(DialogueSpeaker.PLAYER, dialogueManager.getCurrentSpeakerType());
        assertEquals("Yes sir.", dialogueManager.getCurrentLine());
        assertTrue(dialogueManager.isLastLine());

        // Advance -> Ends dialogue
        dialogueManager.advanceDialogue();
        assertFalse(dialogueManager.isActive());
        assertEquals("", dialogueManager.getCurrentSpeaker());
        assertEquals("", dialogueManager.getCurrentLine());
    }

    @Test
    @DisplayName("Handles consecutive lines from the same speaker without forced alternation")
    void testConsecutiveLinesSameSpeaker() {
        NPCConfig config = new NPCConfig("Guard", "Male01_Forward", "consecutive_test", "NONE", false, true, 100f);
        NPC npc = new NPC(config, 100, 100, 35, 57, null);

        dialogueManager.startDialogue(npc);

        // Line 0: NPC
        assertEquals("Guard", dialogueManager.getCurrentSpeaker());
        assertEquals("Line 1 from NPC.", dialogueManager.getCurrentLine());

        // Line 1: NPC (Consecutive)
        dialogueManager.advanceDialogue();
        assertEquals("Guard", dialogueManager.getCurrentSpeaker());
        assertEquals("Line 2 from NPC.", dialogueManager.getCurrentLine());

        // Line 2: Player
        dialogueManager.advanceDialogue();
        assertEquals("Player", dialogueManager.getCurrentSpeaker());
        assertEquals("Line 3 from Player.", dialogueManager.getCurrentLine());

        // Line 3: Player (Consecutive)
        dialogueManager.advanceDialogue();
        assertEquals("Player", dialogueManager.getCurrentSpeaker());
        assertEquals("Line 4 from Player.", dialogueManager.getCurrentLine());
        assertTrue(dialogueManager.isLastLine());

        dialogueManager.advanceDialogue();
        assertFalse(dialogueManager.isActive());
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
