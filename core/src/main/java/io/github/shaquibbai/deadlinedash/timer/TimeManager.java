package io.github.shaquibbai.deadlinedash.timer;

/**
 * TimeManager handles the global countdown timer for Deadline Dash.
 * <p>
 * Game Time Specifications:
 * <ul>
 *   <li>Starting game time: 1 day 24 hours = 48 hours total.</li>
 *   <li>Rate: 30 real-time seconds = 1 game hour.</li>
 *   <li>Total real-time duration: 48 * 30 seconds = 1440.0 seconds.</li>
 *   <li>Time is clamped at 0.0 seconds and never goes negative.</li>
 * </ul>
 * </p>
 */
public class TimeManager {
    public static final float INITIAL_GAME_HOURS = 48.0f;
    public static final float SECONDS_PER_GAME_HOUR = 30.0f;
    public static final float INITIAL_REMAINING_SECONDS = INITIAL_GAME_HOURS * SECONDS_PER_GAME_HOUR; // 1440.0f

    private float remainingSeconds;

    /**
     * Constructs a new TimeManager initialized to 48 game hours (1440.0 real-time seconds).
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
     * Returns the formatted string representation of remaining time, e.g. "1d 23h", "1d 05h", "0d 23h".
     *
     * @return formatted HUD string representation
     */
    public String getFormattedTime() {
        int totalHours = (int) (remainingSeconds / SECONDS_PER_GAME_HOUR);
        int days = totalHours / 24;
        int hours = totalHours % 24;
        return String.format("%dd %02dh", days, hours);
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
     * Resets the timer back to its initial state of 48 game hours (1440.0 seconds).
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
