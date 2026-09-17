package io.github.shaquibbai.deadlinedash.timer;

/**
 * TimeManager handles the global countdown timer for Deadline Dash.
 * <p>
 * Game Time Specifications:
 * <ul>
 *   <li>Starting game time: 36 hours total.</li>
 *   <li>Rate: 30 real-time seconds = 1 game hour.</li>
 *   <li>Total real-time duration: 36 * 30 seconds = 1080.0 seconds (18 real minutes).</li>
 *   <li>Time is clamped at 0.0 seconds and never goes negative.</li>
 * </ul>
 * </p>
 */
public class TimeManager {
    public static final float INITIAL_GAME_HOURS = 36.0f;
    public static final float SECONDS_PER_GAME_HOUR = 30.0f;
    public static final float INITIAL_REMAINING_SECONDS = INITIAL_GAME_HOURS * SECONDS_PER_GAME_HOUR; // 1080.0f (18 real minutes)

    private float remainingSeconds;

    /**
     * Constructs a new TimeManager initialized to 36 game hours (1080.0 real-time seconds = 18 real minutes).
     */
    public TimeManager() {
        this(INITIAL_REMAINING_SECONDS);
    }

    /**
     * Constructs a TimeManager initialized to a specific number of remaining real-time seconds.
     *
     * @param initialSeconds initial remaining time in seconds (clamped >= 0)
     */
    public TimeManager(float initialSeconds) {
        this.remainingSeconds = Math.max(0.0f, initialSeconds);
    }

    /**
     * Updates the countdown timer by advancing real-time delta seconds.
     * Continues counting down regardless of player movement.
     * Clamps remaining time at 0.0 seconds so it never goes negative.
     *
     * @param delta real-time elapsed seconds since last frame
     */
    public void update(float delta) {
        if (delta > 0.0f && remainingSeconds > 0.0f) {
            remainingSeconds -= delta;
            if (remainingSeconds < 0.0f) {
                remainingSeconds = 0.0f;
            }
        }
    }

    /**
     * Returns the current remaining game time in real-time seconds.
     * Starting time is 1440.0 seconds (representing 48 game hours).
     * Every 30 real-time seconds corresponds to 1 game hour.
     *
     * @return current remaining time in real-time seconds (float), clamped >= 0.0f
     */
    public float getTime() {
        return remainingSeconds;
    }

    /**
     * Sets the remaining real-time seconds, clamped at 0.0f.
     *
     * @param seconds remaining time in seconds
     */
    public void setTime(float seconds) {
        this.remainingSeconds = Math.max(0.0f, seconds);
    }

    /**
     * Gets the total remaining game time in game hours.
     *
     * @return remaining game hours (float), where 1 hour = 30 real-time seconds
     */
    public float getRemainingHours() {
        return remainingSeconds / SECONDS_PER_GAME_HOUR;
    }

    /**
     * Returns the days component of the remaining game time.
     * Calculated from total game hours / 24.
     *
     * @return remaining days integer
     */
    public int getDays() {
        int totalHours = (int) (remainingSeconds / SECONDS_PER_GAME_HOUR);
        return totalHours / 24;
    }

    /**
     * Returns the remaining hours component within the current day (0 to 23).
     * Calculated from total game hours % 24.
     *
     * @return remaining hours integer (0..23)
     */
    public int getHours() {
        int totalHours = (int) (remainingSeconds / SECONDS_PER_GAME_HOUR);
        return totalHours % 24;
    }

    /**
     * Quantizes remaining game time to 15 game-minute intervals (7.5 real seconds each).
     * Sequence: 36 : 00 -> 35 : 45 -> 35 : 30 -> 35 : 15 -> 35 : 00 -> ... -> 00 : 00.
     *
     * @return stepped remaining game minutes integer
     */
    public int getSteppedTotalMinutes() {
        if (remainingSeconds <= 0.0f) {
            return 0;
        }
        float secondsPer15Min = SECONDS_PER_GAME_HOUR / 4.0f; // 7.5 real seconds = 15 game minutes
        int intervals = (int) Math.ceil((remainingSeconds - 0.001f) / secondsPer15Min);
        int totalMinutes = intervals * 15;
        int maxMinutes = (int) (INITIAL_GAME_HOURS * 60);
        return Math.min(maxMinutes, Math.max(0, totalMinutes));
    }

    /**
     * Returns the remaining game hours for the HH : MM HUD display, stepped every 15 game minutes (clamped >= 0).
     *
     * @return remaining game hours integer (e.g. 36 down to 0)
     */
    public int getDisplayHours() {
        return getSteppedTotalMinutes() / 60;
    }

    /**
     * Returns the remaining game minutes for the HH : MM HUD display, stepped every 15 game minutes (0, 15, 30, 45).
     *
     * @return remaining game minutes integer (0, 15, 30, or 45)
     */
    public int getDisplayMinutes() {
        return getSteppedTotalMinutes() % 60;
    }

    /**
     * Returns the formatted string representation of remaining time in HH : MM format with two digits each,
     * stepped every 15 game minutes (e.g. "36 : 00", "35 : 45", "35 : 30", "35 : 15", "35 : 00").
     *
     * @return formatted HUD string representation
     */
    public String getFormattedTime() {
        return String.format("%02d : %02d", getDisplayHours(), getDisplayMinutes());
    }

    /**
     * Returns true if the timer has reached zero.
     *
     * @return true if remaining time is 0.0 seconds
     */
    public boolean isExpired() {
        return remainingSeconds <= 0.0f;
    }

    /**
     * Resets the timer back to its initial state of 36 game hours (1080.0 seconds = 18 real minutes).
     */
    public void reset() {
        this.remainingSeconds = INITIAL_REMAINING_SECONDS;
    }

    /**
     * Optional resource disposal for TimeManager system.
     */
    public void dispose() {
        // No heavy resources held currently
    }
}
