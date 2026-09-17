package io.github.shaquibbai.deadlinedash.ending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndingControllerTest {

    private EndingController endingController;

    @BeforeEach
    void setUp() {
        endingController = new EndingController();
    }

    @Test
    @DisplayName("Constants are correctly configured")
    void testConstants() {
        assertEquals(2, EndingController.REQUIRED_TASKS, "Required tasks must be exactly 2");
        assertEquals(15.0f, EndingController.EARLY_WIN_HOUR_THRESHOLD, 0.001f, "Early win threshold must be 15.0f");
    }

    @Test
    @DisplayName("Early win requires strictly > 15.0 hours with exactly 2 completed tasks")
    void testEarlyWinEligibility() {
        // Exactly 2 tasks, > 15.0 hours -> eligible
        assertTrue(endingController.isEarlyWinEligible(2, 20.0f));
        assertTrue(endingController.isEarlyWinEligible(2, 15.01f));

        // Exactly 2 tasks, <= 15.0 hours -> NOT eligible
        assertFalse(endingController.isEarlyWinEligible(2, 15.0f), "Strictly > 15.0f, so 15.0f is NOT early win");
        assertFalse(endingController.isEarlyWinEligible(2, 14.99f));
        assertFalse(endingController.isEarlyWinEligible(2, 0.0f));

        // Less than 2 tasks -> NOT eligible regardless of time
        assertFalse(endingController.isEarlyWinEligible(1, 40.0f));
        assertFalse(endingController.isEarlyWinEligible(0, 48.0f));

        // More than 2 tasks -> NOT eligible (exact == 2 constraint)
        assertFalse(endingController.isEarlyWinEligible(3, 20.0f));
    }

    @Test
    @DisplayName("Timer expired ending evaluates to WIN for 2 tasks and LOSE for fewer")
    void testEvaluateTimerExpiredEnding() {
        assertEquals(EndingType.WIN, endingController.evaluateTimerExpiredEnding(2));
        assertEquals(EndingType.LOSE, endingController.evaluateTimerExpiredEnding(1));
        assertEquals(EndingType.LOSE, endingController.evaluateTimerExpiredEnding(0));
    }

    @Test
    @DisplayName("checkEarlyWin triggers WIN and latches guard flag")
    void testCheckEarlyWinGuard() {
        assertFalse(endingController.isEndingTriggered());
        assertNull(endingController.getTriggeredEnding());

        // Call with non-qualifying
        assertNull(endingController.checkEarlyWin(1, 20.0f));
        assertFalse(endingController.isEndingTriggered());

        // Call with qualifying
        EndingType result = endingController.checkEarlyWin(2, 18.0f);
        assertEquals(EndingType.WIN, result);
        assertTrue(endingController.isEndingTriggered());
        assertEquals(EndingType.WIN, endingController.getTriggeredEnding());

        // Second call must return null and keep guard latched
        assertNull(endingController.checkEarlyWin(2, 18.0f));
        assertNull(endingController.checkTimerExpired(2));
    }

    @Test
    @DisplayName("checkTimerExpired triggers correct ending and latches guard flag")
    void testCheckTimerExpiredGuard() {
        // Lose scenario
        EndingType result = endingController.checkTimerExpired(1);
        assertEquals(EndingType.LOSE, result);
        assertTrue(endingController.isEndingTriggered());
        assertEquals(EndingType.LOSE, endingController.getTriggeredEnding());

        // Second call must return null
        assertNull(endingController.checkTimerExpired(2));
        assertNull(endingController.checkEarlyWin(2, 20.0f));
    }

    @Test
    @DisplayName("Reset clears the guard flag and triggered ending")
    void testReset() {
        endingController.checkEarlyWin(2, 25.0f);
        assertTrue(endingController.isEndingTriggered());

        endingController.reset();
        assertFalse(endingController.isEndingTriggered());
        assertNull(endingController.getTriggeredEnding());
    }
}