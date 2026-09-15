package io.github.shaquibbai.deadlinedash.quest;

import io.github.shaquibbai.deadlinedash.dialogue.DialogueManager;
import io.github.shaquibbai.deadlinedash.inventory.Backpack;
import io.github.shaquibbai.deadlinedash.inventory.Item;
import io.github.shaquibbai.deadlinedash.npc.NPC;
import io.github.shaquibbai.deadlinedash.npc.NPCConfig;
import io.github.shaquibbai.deadlinedash.rep.RepSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestManagerTest {

    private static final String QUEST1_JSON = "{\n" +
        "  \"id\": \"Quest1\",\n" +
        "  \"name\": \"Quest 1\",\n" +
        "  \"description\": \"Deliver the laptop to Rafat\",\n" +
        "  \"rewardRep\": 25,\n" +
        "  \"initialInteractableNpcs\": [\"Rafat\"],\n" +
        "  \"steps\": [\n" +
        "    {\n" +
        "      \"stepIndex\": 1,\n" +
        "      \"npc\": \"Rafat\",\n" +
        "      \"dialogueId\": \"Rafat_intro\",\n" +
        "      \"unlockNpcs\": [\"Sofia\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 2,\n" +
        "      \"npc\": \"Sofia\",\n" +
        "      \"dialogueId\": \"Sofia_intro\",\n" +
        "      \"unlockNpcs\": [\"Shopkeeper\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 3,\n" +
        "      \"npc\": \"Shopkeeper\",\n" +
        "      \"dialogueId\": \"Shopkeeper_intro\",\n" +
        "      \"addItem\": \"Pizza\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 4,\n" +
        "      \"npc\": \"Sofia\",\n" +
        "      \"dialogueId\": \"Sofia_pizza\",\n" +
        "      \"requiredItem\": \"Pizza\",\n" +
        "      \"addItem\": \"Laptop\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 5,\n" +
        "      \"npc\": \"Rafat\",\n" +
        "      \"dialogueId\": \"Rafat_laptop\",\n" +
        "      \"requiredItem\": \"Laptop\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    private QuestManager questManager;
    private Backpack backpack;
    private RepSystem repSystem;

    @BeforeEach
    void setUp() {
        questManager = new QuestManager();
        questManager.loadQuestFromString(QUEST1_JSON);
        backpack = new Backpack();
        repSystem = new RepSystem(20);
    }

    @Test
    @DisplayName("Initial Quest 1 state: Rafat interactable, Sofia and Shopkeeper locked")
    void testInitialQuestState() {
        assertEquals(QuestState.IN_PROGRESS, questManager.getQuestState());
        assertEquals(1, questManager.getCurrentStepIndex());

        assertTrue(questManager.isNpcInteractable("Rafat"));
        assertFalse(questManager.isNpcInteractable("Sofia"));
        assertFalse(questManager.isNpcInteractable("Shopkeeper"));
    }

    @Test
    @DisplayName("Step 1 completion (Rafat_intro) unlocks Sofia")
    void testStep1CompletesAndUnlocksSofia() {
        boolean handled = questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        assertTrue(handled);
        assertEquals(2, questManager.getCurrentStepIndex());

        assertTrue(questManager.isNpcInteractable("Rafat"));
        assertTrue(questManager.isNpcInteractable("Sofia"));
        assertFalse(questManager.isNpcInteractable("Shopkeeper"));
    }

    @Test
    @DisplayName("Step 2 completion (Sofia_intro) unlocks Shopkeeper")
    void testStep2CompletesAndUnlocksShopkeeper() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);

        boolean handled = questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        assertTrue(handled);
        assertEquals(3, questManager.getCurrentStepIndex());

        assertTrue(questManager.isNpcInteractable("Shopkeeper"));
    }

    @Test
    @DisplayName("Step 3 completion (Shopkeeper_intro) adds Pizza to Backpack upon dialogue finish")
    void testStep3AddsPizzaToBackpack() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);

        assertFalse(backpack.hasItem(new Item("Pizza")));

        boolean handled = questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        assertTrue(handled);
        assertEquals(4, questManager.getCurrentStepIndex());

        assertTrue(backpack.hasItem(new Item("Pizza")));
    }

    @Test
    @DisplayName("Step 4 dialogue resolution and completion: requires Pizza, adds Laptop, retains Pizza")
    void testStep4RequiresPizzaAndAddsLaptop() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);

        // Before Step 3 (no Pizza in backpack)
        assertEquals("Sofia_intro", questManager.getDialogueForNpc("Sofia", "Sofia_intro", backpack));
        assertFalse(questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem));

        // Complete Step 3 (add Pizza)
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        assertTrue(backpack.hasItem(new Item("Pizza")));

        // Now step 4 eligible
        assertEquals("Sofia_pizza", questManager.getDialogueForNpc("Sofia", "Sofia_intro", backpack));

        boolean handled = questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);
        assertTrue(handled);
        assertEquals(5, questManager.getCurrentStepIndex());

        assertTrue(backpack.hasItem(new Item("Laptop")));
        assertTrue(backpack.hasItem(new Item("Pizza")));
    }

    @Test
    @DisplayName("Step 5 completion (Rafat_laptop) completes Quest 1 and awards +25 REP exactly once")
    void testStep5CompletesQuestAndAwardsRepOnce() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);

        assertEquals(20, repSystem.getRep());
        assertEquals("Rafat_laptop", questManager.getDialogueForNpc("Rafat", "Rafat_intro", backpack));

        boolean completed = questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);
        assertTrue(completed);
        assertTrue(questManager.isQuestCompleted());
        assertEquals(45, repSystem.getRep()); // 20 + 25

        // Subsequent interaction does not award REP again
        boolean secondCall = questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);
        assertFalse(secondCall);
        assertEquals(45, repSystem.getRep());
    }

    @Test
    @DisplayName("Integration with DialogueManager: Rewards execute when dialogue finishes completely")
    void testDialogueManagerIntegration() {
        String testDialogueJson = "{\n" +
            "  \"Shopkeeper_intro\": {\n" +
            "    \"lines\": [\n" +
            "      { \"speaker\": \"npc\", \"text\": \"Here is your Pizza!\" }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        DialogueManager dialogueManager = new DialogueManager();
        dialogueManager.loadDialoguesFromString(testDialogueJson);

        dialogueManager.setCompletionListener((dialogueId, npc) -> {
            String npcName = npc != null ? npc.getName() : "";
            questManager.onDialogueCompleted(dialogueId, npcName, backpack, repSystem);
        });

        // Fast forward quest to step 3
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);

        NPCConfig shopConfig = new NPCConfig("Shopkeeper", "Male01_Left", "Shopkeeper_intro", "NONE", false, true, 100f);
        NPC shopkeeper = new NPC(shopConfig, 0, 0, 40, 60, null);

        dialogueManager.startDialogue(shopkeeper, "Shopkeeper_intro");
        assertTrue(dialogueManager.isActive());
        assertFalse(backpack.hasItem(new Item("Pizza")));

        // Advance past final line
        dialogueManager.advanceDialogue();
        assertFalse(dialogueManager.isActive());
        assertTrue(backpack.hasItem(new Item("Pizza")));
    }
}
