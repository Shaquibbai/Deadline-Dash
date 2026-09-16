package io.github.shaquibbai.deadlinedash.quest;

import io.github.shaquibbai.deadlinedash.inventory.Backpack;
import io.github.shaquibbai.deadlinedash.rep.RepSystem;
import io.github.shaquibbai.deadlinedash.timer.TimeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Quest2Test {

    private Quest2Controller quest2Controller;
    private Backpack backpack;
    private RepSystem repSystem;
    private TimeManager timeManager;

    @BeforeEach
    void setUp() {
        quest2Controller = new Quest2Controller();
        backpack = new Backpack();
        repSystem = new RepSystem(20);
        timeManager = new TimeManager(1440.0f); // 48 hours
    }

    @Test
    @DisplayName("Initial state: Shafeen has Yellow !, Talha is locked")
    void testInitialShafeenState() {
        assertTrue(quest2Controller.isNpcInteractable("Shafeen"));
        assertFalse(quest2Controller.isNpcInteractable("Talha"));
        assertEquals(QuestMarker.NEW_INTERACTION, quest2Controller.getMarkerForNpc("Shafeen", backpack));
        assertEquals(QuestMarker.NONE, quest2Controller.getMarkerForNpc("Talha", backpack));
        assertEquals("Shafeen_intro", quest2Controller.getDialogueForNpc("Shafeen", "Shafeen_intro", backpack));
    }

    @Test
    @DisplayName("REGRESSION TEST 1: Shafeen intro cannot be triggered twice")
    void testShafeenIntroCannotBeTriggeredTwice() {
        // First completion of Shafeen_intro
        boolean showPrompt = quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);
        assertFalse(showPrompt);

        // Shafeen MUST be in waiting state (Shafeen_waiting, Red ?)
        assertEquals(Quest2Controller.State.TALHA_INTRO, quest2Controller.getState());
        assertEquals(QuestMarker.REMINDER, quest2Controller.getMarkerForNpc("Shafeen", backpack));
        assertEquals("Shafeen_waiting", quest2Controller.getDialogueForNpc("Shafeen", "Shafeen_intro", backpack));

        // Attempting to trigger Shafeen_intro again must NOT revert state to SHAFEEN_INTRO
        quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);
        assertEquals(Quest2Controller.State.TALHA_INTRO, quest2Controller.getState());
        assertEquals(QuestMarker.REMINDER, quest2Controller.getMarkerForNpc("Shafeen", backpack));
        assertEquals("Shafeen_waiting", quest2Controller.getDialogueForNpc("Shafeen", "Shafeen_intro", backpack));
    }

    @Test
    @DisplayName("REGRESSION TEST 2: Talha correctly changes from Talha_intro/! to Talha_waiting/? after No")
    void testTalhaNoChoiceTransitionsToWaitingAndRedQuestion() {
        quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);

        // Before Talha_intro is read
        assertTrue(quest2Controller.isNpcInteractable("Talha"));
        assertEquals(QuestMarker.NEW_INTERACTION, quest2Controller.getMarkerForNpc("Talha", backpack));
        assertEquals("Talha_intro", quest2Controller.getDialogueForNpc("Talha", "Talha_intro", backpack));

        // Talha_intro finishes -> prompt appears
        boolean showPrompt = quest2Controller.onDialogueCompleted("Talha_intro", "Talha", backpack, repSystem, timeManager);
        assertTrue(showPrompt);
        assertTrue(quest2Controller.isMatchPromptActive());

        // Player chooses NO
        quest2Controller.onMatchChoiceNo();
        assertFalse(quest2Controller.isMatchPromptActive());

        // State MUST be TALHA_WAITING: interactable = true, dialogue = Talha_waiting, marker = Red ?
        assertEquals(Quest2Controller.State.TALHA_WAITING, quest2Controller.getState());
        assertTrue(quest2Controller.isNpcInteractable("Talha"));
        assertEquals(QuestMarker.REMINDER, quest2Controller.getMarkerForNpc("Talha", backpack));
        assertEquals("Talha_waiting", quest2Controller.getDialogueForNpc("Talha", "Talha_intro", backpack));

        // Next interaction MUST start Talha_waiting, NEVER Talha_intro
        assertNotEquals("Talha_intro", quest2Controller.getDialogueForNpc("Talha", "Talha_intro", backpack));
        assertEquals("Talha_waiting", quest2Controller.getDialogueForNpc("Talha", "Talha_intro", backpack));

        // Choosing NO from Talha_waiting leaves him in the exact same waiting state with Red ?
        boolean showPromptAgain = quest2Controller.onDialogueCompleted("Talha_waiting", "Talha", backpack, repSystem, timeManager);
        assertTrue(showPromptAgain);
        quest2Controller.onMatchChoiceNo();

        assertEquals(Quest2Controller.State.TALHA_WAITING, quest2Controller.getState());
        assertTrue(quest2Controller.isNpcInteractable("Talha"));
        assertEquals(QuestMarker.REMINDER, quest2Controller.getMarkerForNpc("Talha", backpack));
        assertEquals("Talha_waiting", quest2Controller.getDialogueForNpc("Talha", "Talha_intro", backpack));
    }

    @Test
    @DisplayName("REGRESSION TEST 3: Completed Quest 2 cannot interact with Talha or launch the football minigame again")
    void testCompletedQuest2CannotInteractOrStartMatch() {
        quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);
        quest2Controller.onDialogueCompleted("Talha_intro", "Talha", backpack, repSystem, timeManager);
        quest2Controller.onMatchChoiceYes();

        // Match result -> CSE wins
        quest2Controller.handleMatchResult(3, 1);
        quest2Controller.onDialogueCompleted("Talha_matchWon", "Talha", backpack, repSystem, timeManager);

        // Quest 2 MUST be completed
        assertTrue(quest2Controller.isQuestCompleted());
        assertEquals(Quest2Controller.State.COMPLETED, quest2Controller.getState());

        // Shafeen and Talha MUST be interactable = false and markers = NONE
        assertFalse(quest2Controller.isNpcInteractable("Shafeen"));
        assertFalse(quest2Controller.isNpcInteractable("Talha"));
        assertEquals(QuestMarker.NONE, quest2Controller.getMarkerForNpc("Shafeen", backpack));
        assertEquals(QuestMarker.NONE, quest2Controller.getMarkerForNpc("Talha", backpack));
        assertFalse(quest2Controller.isMatchPromptActive());

        // Interacting with Talha after completion must do absolutely nothing
        boolean promptResult = quest2Controller.onDialogueCompleted("Talha_intro", "Talha", backpack, repSystem, timeManager);
        assertFalse(promptResult);
        assertFalse(quest2Controller.isMatchPromptActive());

        boolean waitingResult = quest2Controller.onDialogueCompleted("Talha_waiting", "Talha", backpack, repSystem, timeManager);
        assertFalse(waitingResult);
        assertFalse(quest2Controller.isMatchPromptActive());

        // Repeated reward attempt does nothing
        quest2Controller.completeQuestWithResult(true, repSystem, timeManager);
        assertEquals(50, repSystem.getRep()); // 20 + 30 (for diff 2)
        assertEquals(1290.0f, timeManager.getTime(), 0.01f); // 1440 - 150
    }

    @Test
    @DisplayName("Match result CSE wins: goal diff 1 -> +20 REP, -5 hours after Talha_matchWon completes")
    void testMatchResultCseWinDiff1() {
        quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);
        quest2Controller.onDialogueCompleted("Talha_intro", "Talha", backpack, repSystem, timeManager);

        String nextDialogue = quest2Controller.handleMatchResult(2, 1); // Goal diff = 1
        assertEquals("Talha_matchWon", nextDialogue);

        assertFalse(quest2Controller.isQuestCompleted());
        assertEquals(20, repSystem.getRep());
        assertEquals(1440.0f, timeManager.getTime(), 0.01f);

        // Dialogue Talha_matchWon finishes
        quest2Controller.onDialogueCompleted("Talha_matchWon", "Talha", backpack, repSystem, timeManager);

        assertTrue(quest2Controller.isQuestCompleted());
        assertEquals(40, repSystem.getRep()); // 20 + 20
        assertEquals(1290.0f, timeManager.getTime(), 0.01f); // 1440 - 150 (5 game hours)
    }

    @Test
    @DisplayName("Match result CSE loses: goal diff 2 -> -20 REP, -10 hours after Talha_matchLost completes")
    void testMatchResultCseLossDiff2() {
        quest2Controller.onDialogueCompleted("Shafeen_intro", "Shafeen", backpack, repSystem, timeManager);
        quest2Controller.onDialogueCompleted("Talha_intro", "Talha", backpack, repSystem, timeManager);

        String nextDialogue = quest2Controller.handleMatchResult(1, 3); // Goal diff = 2
        assertEquals("Talha_matchLost", nextDialogue);

        assertFalse(quest2Controller.isQuestCompleted());

        // Dialogue Talha_matchLost finishes
        quest2Controller.onDialogueCompleted("Talha_matchLost", "Talha", backpack, repSystem, timeManager);

        assertTrue(quest2Controller.isQuestCompleted());
        assertEquals(0, repSystem.getRep()); // 20 - 20
        assertEquals(1140.0f, timeManager.getTime(), 0.01f); // 1440 - 300 (10 game hours)
    }
}
