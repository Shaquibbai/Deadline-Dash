package io.github.shaquibbai.deadlinedash.ending;

/**
 * Controller responsible for evaluating game ending conditions.
 * Determines when the player qualifies for an Early Win (>15 game hours remaining)
 * or evaluates WIN vs LOSE when the game countdown timer reaches zero.
 * Includes a guard flag to prevent multiple ending triggers.
 */
public class EndingController {
    public static final int REQUIRED_TASKS = 3;
    public static final float EARLY_WIN_HOUR_THRESHOLD = 15.0f;

    private boolean endingTriggered = false;
    private EndingType triggeredEnding = null;

    public EndingController() {
    }

    /**
     * Evaluates if current state qualifies strictly for an early win.
     * Condition: Exactly REQUIRED_TASKS completed AND strictly more than 15 game hours remaining.
     */
    public boolean isEarlyWinEligible(int completedTasks, float remainingHours) {
        return completedTasks == REQUIRED_TASKS && remainingHours > EARLY_WIN_HOUR_THRESHOLD;
    }

    /**
     * Evaluates the ending type upon timer expiration.
     * If exactly REQUIRED_TASKS are completed, WIN; otherwise LOSE.
     */
    public EndingType evaluateTimerExpiredEnding(int completedTasks) {
        if (completedTasks == REQUIRED_TASKS) {
            return EndingType.WIN;
        } else {
            return EndingType.LOSE;
        }
    }

    /**
     * Checks early win condition and latches the ending trigger guard if qualified.
     * Returns EndingType.WIN if triggered on this check, or null otherwise.
     */
    public EndingType checkEarlyWin(int completedTasks, float remainingHours) {
        if (endingTriggered) {
            return null;
        }
        if (isEarlyWinEligible(completedTasks, remainingHours)) {
            endingTriggered = true;
            triggeredEnding = EndingType.WIN;
            return EndingType.WIN;
        }
        return null;
    }

    /**
     * Checks timer-expired ending and latches the ending trigger guard.
     * Returns the determined EndingType (WIN or LOSE) if triggered on this check, or null if already triggered.
     */
    public EndingType checkTimerExpired(int completedTasks) {
        if (endingTriggered) {
            return null;
        }
        endingTriggered = true;
        triggeredEnding = evaluateTimerExpiredEnding(completedTasks);
        return triggeredEnding;
    }

    public boolean isEndingTriggered() {
        return endingTriggered;
    }

    public EndingType getTriggeredEnding() {
        return triggeredEnding;
    }

    public void reset() {
        endingTriggered = false;
        triggeredEnding = null;
    }
}
