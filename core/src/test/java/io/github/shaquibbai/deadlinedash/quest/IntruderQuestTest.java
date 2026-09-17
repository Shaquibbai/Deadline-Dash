package io.github.shaquibbai.deadlinedash.quest;

import io.github.shaquibbai.deadlinedash.rep.RepSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntruderQuestTest {
    private IntruderQuestController controller;
    private RepSystem repSystem;

    @BeforeEach
    public void setUp() {
        controller = new IntruderQuestController();
        repSystem = new RepSystem();
    }

    @Test
    public void testInitialization() {
        assertEquals(IntruderQuestController.State.NOT_STARTED, controller.getState());
        assertFalse(controller.isQuestCompleted());
        assertFalse(controller.isStartPromptActive());
        assertEquals(5, controller.getTotalIntruders());
        assertEquals(0, controller.getDetectedIntrudersCount());
        assertEquals(5, controller.getRemainingIntrudersCount());
        assertEquals(10, controller.getNpcs().size());

        // Check 5 valid NPCs (even)
        assertFalse(controller.getNpc("Tanvir").isIntruder());
        assertEquals(124, controller.getNpc("Tanvir").getRegistrationNumber());
        assertFalse(controller.getNpc("Farhan").isIntruder());
        assertEquals(208, controller.getNpc("Farhan").getRegistrationNumber());
        assertFalse(controller.getNpc("Aayan").isIntruder());
        assertEquals(316, controller.getNpc("Aayan").getRegistrationNumber());
        assertFalse(controller.getNpc("Nafis").isIntruder());
        assertEquals(422, controller.getNpc("Nafis").getRegistrationNumber());
        assertFalse(controller.getNpc("Sami").isIntruder());
        assertEquals(276, controller.getNpc("Sami").getRegistrationNumber());

        // Check 5 intruder NPCs (odd)
        assertTrue(controller.getNpc("Shadman").isIntruder());
        assertEquals(137, controller.getNpc("Shadman").getRegistrationNumber());
        assertTrue(controller.getNpc("Kazi").isIntruder());
        assertEquals(151, controller.getNpc("Kazi").getRegistrationNumber());
        assertTrue(controller.getNpc("Adnan").isIntruder());
        assertEquals(167, controller.getNpc("Adnan").getRegistrationNumber());
        assertTrue(controller.getNpc("Fahim").isIntruder());
        assertEquals(189, controller.getNpc("Fahim").getRegistrationNumber());
        assertTrue(controller.getNpc("Tariq").isIntruder());
        assertEquals(205, controller.getNpc("Tariq").getRegistrationNumber());
    }

    @Test
    public void testQuestGiverMarkersAndStartPrompt() {
        // Before quest start
        assertEquals(QuestMarker.QUEST_AVAILABLE, controller.getMarkerForNpc("Shaquib"));
        assertEquals(IntruderQuestController.DIALOGUE_GIVER_INTRO, controller.getDialogueForNpc("Shaquib", "default"));

        // Finish intro dialogue
        boolean prompted = controller.onDialogueCompleted(IntruderQuestController.DIALOGUE_GIVER_INTRO, "Shaquib", repSystem);
        assertTrue(prompted);
        assertTrue(controller.isStartPromptActive());

        // Say NO
        controller.onStartChoiceNo();
        assertFalse(controller.isStartPromptActive());
        assertEquals(IntruderQuestController.State.NOT_STARTED, controller.getState());

        // Trigger prompt again and say YES
        controller.onDialogueCompleted(IntruderQuestController.DIALOGUE_GIVER_INTRO, "Shaquib", repSystem);
        controller.onStartChoiceYes();
        assertEquals(IntruderQuestController.State.INVESTIGATION, controller.getState());
        assertEquals(QuestMarker.NONE, controller.getMarkerForNpc("Shaquib"));
        assertEquals(IntruderQuestController.DIALOGUE_GIVER_WAITING, controller.getDialogueForNpc("Shaquib", "default"));
    }

    @Test
    public void testValidNpcInvestigation() {
        controller.onStartChoiceYes();

        // Check initial marker on Tanvir
        assertEquals(QuestMarker.QUEST_AVAILABLE, controller.getMarkerForNpc("Tanvir"));
        assertEquals("intruder_npc_124", controller.getDialogueForNpc("Tanvir", "default"));

        // Talk to Tanvir (Valid, even 124)
        boolean isIntruder = controller.onDialogueCompleted("intruder_npc_124", "Tanvir", repSystem);
        assertFalse(isIntruder);
        assertTrue(controller.getNpc("Tanvir").isInvestigated());
        assertEquals(QuestMarker.VALID, controller.getMarkerForNpc("Tanvir"));
        assertEquals(5, controller.getRemainingIntrudersCount());
        assertFalse(controller.isIntruderAlertActive());

        // Re-talking returns already checked dialogue and preserves marker
        assertEquals(IntruderQuestController.DIALOGUE_ALREADY_CHECKED, controller.getDialogueForNpc("Tanvir", "default"));
        assertEquals(QuestMarker.VALID, controller.getMarkerForNpc("Tanvir"));
        assertEquals(5, controller.getRemainingIntrudersCount());
    }

    @Test
    public void testIntruderDetectionAndAlert() {
        controller.onStartChoiceYes();

        // Check initial marker on Shadman
        assertEquals(QuestMarker.QUEST_AVAILABLE, controller.getMarkerForNpc("Shadman"));
        assertEquals("intruder_npc_137", controller.getDialogueForNpc("Shadman", "default"));

        // Talk to Shadman (Intruder, odd 137)
        boolean isIntruder = controller.onDialogueCompleted("intruder_npc_137", "Shadman", repSystem);
        assertTrue(isIntruder);
        assertTrue(controller.getNpc("Shadman").isInvestigated());
        assertEquals(QuestMarker.INTRUDER, controller.getMarkerForNpc("Shadman"));
        assertEquals(1, controller.getDetectedIntrudersCount());
        assertEquals(4, controller.getRemainingIntrudersCount());
        assertTrue(controller.isIntruderAlertActive());
        assertEquals("Shadman", controller.getLastAlertNpcName());
        assertEquals(137, controller.getLastAlertRegNumber());
        assertEquals(1, controller.getLastAlertFoundCount());

        controller.dismissIntruderAlert();
        assertFalse(controller.isIntruderAlertActive());

        // Retalking to Shadman should not decrement counter again
        controller.onDialogueCompleted("intruder_npc_137", "Shadman", repSystem);
        assertEquals(4, controller.getRemainingIntrudersCount());
    }

    @Test
    public void testFullQuestCompletionFlow() {
        controller.onStartChoiceYes();
        int initialRep = repSystem.getRep();

        // Talk to 5 Valid NPCs
        controller.onDialogueCompleted("intruder_npc_124", "Tanvir", repSystem);
        controller.onDialogueCompleted("intruder_npc_208", "Farhan", repSystem);
        controller.onDialogueCompleted("intruder_npc_316", "Aayan", repSystem);
        controller.onDialogueCompleted("intruder_npc_422", "Nafis", repSystem);
        controller.onDialogueCompleted("intruder_npc_276", "Sami", repSystem);

        assertEquals(0, controller.getDetectedIntrudersCount());
        assertEquals(5, controller.getRemainingIntrudersCount());

        // Find 1st Intruder
        controller.onDialogueCompleted("intruder_npc_137", "Shadman", repSystem);
        assertEquals(4, controller.getRemainingIntrudersCount());
        assertFalse(controller.isQuestCompleted());

        // Find 2nd Intruder
        controller.onDialogueCompleted("intruder_npc_151", "Kazi", repSystem);
        assertEquals(3, controller.getRemainingIntrudersCount());

        // Find 3rd Intruder
        controller.onDialogueCompleted("intruder_npc_167", "Adnan", repSystem);
        assertEquals(2, controller.getRemainingIntrudersCount());

        // Find 4th Intruder
        controller.onDialogueCompleted("intruder_npc_189", "Fahim", repSystem);
        assertEquals(1, controller.getRemainingIntrudersCount());

        // Find 5th Intruder
        controller.onDialogueCompleted("intruder_npc_205", "Tariq", repSystem);
        assertEquals(0, controller.getRemainingIntrudersCount());
        assertEquals(5, controller.getDetectedIntrudersCount());

        // Quest completed!
        assertTrue(controller.isQuestCompleted());
        assertTrue(controller.isCompletionBannerActive());
        assertEquals(initialRep + 35, repSystem.getRep());
        assertEquals(IntruderQuestController.DIALOGUE_GIVER_COMPLETE, controller.getDialogueForNpc("Shaquib", "default"));

        controller.dismissCompletionBanner();
        assertFalse(controller.isCompletionBannerActive());
    }
}
