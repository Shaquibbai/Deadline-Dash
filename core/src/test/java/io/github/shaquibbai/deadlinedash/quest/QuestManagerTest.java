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
        "  \"description\": \"Deliver the laptop and gift package to Rafat\",\n" +
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
        "      \"reminderDialogueId\": \"Rafat_waiting\",\n" +
        "      \"reminderNpcs\": [\"Rafat\"],\n" +
        "      \"unlockNpcs\": [\"Shopkeeper\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 3,\n" +
        "      \"npc\": \"Shopkeeper\",\n" +
        "      \"dialogueId\": \"Shopkeeper_intro\",\n" +
        "      \"addItem\": \"Pizza\",\n" +
        "      \"addItemMessage\": \"Pizza added to backpack\",\n" +
        "      \"reminderDialogueId\": \"Sofia_waiting\",\n" +
        "      \"reminderNpcs\": [\"Rafat\", \"Sofia\"],\n" +
        "      \"lockNpcs\": [\"Shopkeeper\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 4,\n" +
        "      \"npc\": \"Sofia\",\n" +
        "      \"dialogueId\": \"Sofia_pizza\",\n" +
        "      \"reminderDialogueId\": \"Sofia_waiting\",\n" +
        "      \"requiredItem\": \"Pizza\",\n" +
        "      \"removeItem\": \"Pizza\",\n" +
        "      \"addItem\": \"Laptop\",\n" +
        "      \"addItemMessage\": \"Laptop added to backpack\",\n" +
        "      \"unlockNpcs\": [\"Deliveryman\"],\n" +
        "      \"reminderNpcs\": [\"Rafat\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 5,\n" +
        "      \"npc\": \"Deliveryman\",\n" +
        "      \"dialogueId\": \"Deliveryman_intro\",\n" +
        "      \"addItem\": \"Gift\",\n" +
        "      \"addItemMessage\": \"Gift added to backpack\",\n" +
        "      \"reminderDialogueId\": \"Rafat_waiting\",\n" +
        "      \"reminderNpcs\": [\"Rafat\"],\n" +
        "      \"lockNpcs\": [\"Deliveryman\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"stepIndex\": 6,\n" +
        "      \"npc\": \"Rafat\",\n" +
        "      \"dialogueId\": \"Rafat_laptop\",\n" +
        "      \"reminderDialogueId\": \"Rafat_waiting\",\n" +
        "      \"requiredItems\": [\"Laptop\", \"Gift\"],\n" +
        "      \"removeItems\": [\"Laptop\", \"Gift\"]\n" +
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
    @DisplayName("Initial state: Deliveryman is locked, Rafat has yellow !")
    void testInitialStateDeliverymanLocked() {
        assertFalse(questManager.isNpcInteractable("Deliveryman"));
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Deliveryman", backpack));
        assertEquals(QuestMarker.NEW_INTERACTION, questManager.getMarkerForNpc("Rafat", backpack));
    }

    @Test
    @DisplayName("Completing Sofia_pizza unlocks Deliveryman with Yellow !")
    void testSofiaPizzaUnlocksDeliveryman() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);

        assertFalse(questManager.isNpcInteractable("Deliveryman"));

        String toast = questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);
        assertEquals("Laptop added to backpack", toast);

        // Pizza removed, Laptop added
        assertFalse(backpack.hasItem(new Item("Pizza")));
        assertTrue(backpack.hasItem(new Item("Laptop")));

        // Deliveryman unlocked with Yellow !
        assertTrue(questManager.isNpcInteractable("Deliveryman"));
        assertEquals(QuestMarker.NEW_INTERACTION, questManager.getMarkerForNpc("Deliveryman", backpack));
        assertEquals("Deliveryman_intro", questManager.getDialogueForNpc("Deliveryman", "Deliveryman_intro", backpack));
    }

    @Test
    @DisplayName("Deliveryman_intro adds Gift once, shows toast, locks Deliveryman, and gives Rafat Yellow !")
    void testDeliverymanIntroGivesGiftAndLocksDeliveryman() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);

        assertFalse(backpack.hasItem(new Item("Gift")));

        String toast = questManager.onDialogueCompleted("Deliveryman_intro", "Deliveryman", backpack, repSystem);
        assertEquals("Gift added to backpack", toast);

        assertTrue(backpack.hasItem(new Item("Gift")));
        assertTrue(backpack.hasItem(new Item("Laptop")));

        // Deliveryman disabled immediately
        assertFalse(questManager.isNpcInteractable("Deliveryman"));
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Deliveryman", backpack));

        // Rafat now has Yellow ! because both Laptop and Gift are present
        assertEquals(QuestMarker.NEW_INTERACTION, questManager.getMarkerForNpc("Rafat", backpack));
        assertEquals("Rafat_laptop", questManager.getDialogueForNpc("Rafat", "Rafat_intro", backpack));
    }

    @Test
    @DisplayName("Rafat cannot complete final step with missing Laptop or missing Gift")
    void testRafatCannotCompleteWithMissingItem() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Deliveryman_intro", "Deliveryman", backpack, repSystem);

        // Remove Laptop to test missing Laptop
        backpack.removeItem(new Item("Laptop"));

        assertEquals(QuestMarker.REMINDER, questManager.getMarkerForNpc("Rafat", backpack));
        assertEquals("Rafat_waiting", questManager.getDialogueForNpc("Rafat", "Rafat_intro", backpack));

        String toast = questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);
        assertNull(toast);
        assertFalse(questManager.isQuestCompleted());
        assertEquals(20, repSystem.getRep());

        // Restore Laptop, remove Gift to test missing Gift
        backpack.addItem(new Item("Laptop"));
        backpack.removeItem(new Item("Gift"));

        assertEquals(QuestMarker.REMINDER, questManager.getMarkerForNpc("Rafat", backpack));
        assertEquals("Rafat_waiting", questManager.getDialogueForNpc("Rafat", "Rafat_intro", backpack));

        toast = questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);
        assertNull(toast);
        assertFalse(questManager.isQuestCompleted());
        assertEquals(20, repSystem.getRep());
    }

    @Test
    @DisplayName("Rafat_laptop completes quest when both Laptop and Gift are present, removing both and awarding +25 REP once")
    void testRafatFinalStepCompletion() {
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Deliveryman_intro", "Deliveryman", backpack, repSystem);

        assertTrue(backpack.hasItem(new Item("Laptop")));
        assertTrue(backpack.hasItem(new Item("Gift")));
        assertEquals(20, repSystem.getRep());

        questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);

        // BOTH items MUST be removed
        assertFalse(backpack.hasItem(new Item("Laptop")));
        assertFalse(backpack.hasItem(new Item("Gift")));

        assertTrue(questManager.isQuestCompleted());
        assertEquals(45, repSystem.getRep()); // 20 + 25

        // All markers hidden & quest interactions disabled
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Rafat", backpack));
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Sofia", backpack));
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Shopkeeper", backpack));
        assertEquals(QuestMarker.NONE, questManager.getMarkerForNpc("Deliveryman", backpack));

        assertFalse(questManager.isNpcInteractable("Rafat"));
        assertFalse(questManager.isNpcInteractable("Sofia"));
        assertFalse(questManager.isNpcInteractable("Shopkeeper"));
        assertFalse(questManager.isNpcInteractable("Deliveryman"));

        // Repeated completion attempt gives no additional REP or items
        questManager.onDialogueCompleted("Rafat_laptop", "Rafat", backpack, repSystem);
        assertEquals(45, repSystem.getRep());
    }

    @Test
    @DisplayName("Integration with DialogueManager: Rewards execute only when Deliveryman_intro finishes completely")
    void testDialogueManagerDeliverymanCompletion() {
        String testDialogueJson = "{\n" +
            "  \"Deliveryman_intro\": {\n" +
            "    \"lines\": [\n" +
            "      { \"speaker\": \"npc\", \"text\": \"Here is your package!\" }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        DialogueManager dialogueManager = new DialogueManager();
        dialogueManager.loadDialoguesFromString(testDialogueJson);

        dialogueManager.setCompletionListener((dialogueId, npc) -> {
            String npcName = npc != null ? npc.getName() : "";
            questManager.onDialogueCompleted(dialogueId, npcName, backpack, repSystem);
        });

        // Fast forward to step 5
        questManager.onDialogueCompleted("Rafat_intro", "Rafat", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_intro", "Sofia", backpack, repSystem);
        questManager.onDialogueCompleted("Shopkeeper_intro", "Shopkeeper", backpack, repSystem);
        questManager.onDialogueCompleted("Sofia_pizza", "Sofia", backpack, repSystem);

        NPCConfig deliveryConfig = new NPCConfig("Deliveryman", "Male01_Left", "Deliveryman_intro", "NONE", false, true, 100f);
        NPC deliveryman = new NPC(deliveryConfig, 0, 0, 40, 60, null);

        dialogueManager.startDialogue(deliveryman, "Deliveryman_intro");
        assertTrue(dialogueManager.isActive());
        assertFalse(backpack.hasItem(new Item("Gift")));

        // Advance past final line
        dialogueManager.advanceDialogue();
        assertFalse(dialogueManager.isActive());
        assertTrue(backpack.hasItem(new Item("Gift")));
    }
}
