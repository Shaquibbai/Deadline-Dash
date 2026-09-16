package io.github.shaquibbai.deadlinedash.timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeManagerTest {

    private TimeManager timeManager;

    @BeforeEach
    void setUp() {
        timeManager = new TimeManager();
    }

    @Test
    @DisplayName("Initial game time must equal 48 hours (1440 real-time seconds)")
    void testInitialTime() {
        assertEquals(1440.0f, timeManager.getTime(), 0.001f, "Initial time returned by getTime() must be 1440.0 seconds");
        assertEquals(48.0f, timeManager.getRemainingHours(), 0.001f, "Initial remaining hours must be 48.0");
        assertEquals(2, timeManager.getDays(), "Initial remaining days must be 2");
        assertEquals(0, timeManager.getHours(), "Initial remaining hours component must be 0");
    }

    @Test
    @DisplayName("30 seconds update reduces game time by exactly 1 hour")
    void testSingleIntervalReduction() {
        timeManager.update(30.0f);

        assertEquals(1410.0f, timeManager.getTime(), 0.001f, "After 30 seconds, remaining real-time seconds should be 1410.0");
        assertEquals(47.0f, timeManager.getRemainingHours(), 0.001f, "After 30 seconds, remaining hours should be 47.0");
        assertEquals(1, timeManager.getDays(), "47 hours corresponds to 1 day");
        assertEquals(23, timeManager.getHours(), "47 hours corresponds to 23 hours in remaining day");
        assertEquals("1d 23h", timeManager.getFormattedTime(), "Formatted string should be 1d 23h");
    }

    @Test
    @DisplayName("Multiple 30-second intervals reduce game time correctly")
    void testMultipleIntervalsReduction() {
        // 3 intervals of 30 seconds = 90 seconds total (3 game hours)
        timeManager.update(30.0f);
        timeManager.update(30.0f);
        timeManager.update(30.0f);

        assertEquals(1350.0f, timeManager.getTime(), 0.001f, "After 90 seconds, remaining time should be 1350.0 seconds");
        assertEquals(45.0f, timeManager.getRemainingHours(), 0.001f, "After 90 seconds, remaining hours should be 45.0");
        assertEquals("1d 21h", timeManager.getFormattedTime());
    }

    @Test
    @DisplayName("Timer never goes below 0 when updated beyond total duration")
    void testTimerClampingAtZero() {
        // Update by 2000 seconds (more than total duration 1440s)
        timeManager.update(2000.0f);

        assertEquals(0.0f, timeManager.getTime(), 0.001f, "Timer must clamp at 0.0 seconds");
        assertEquals(0.0f, timeManager.getRemainingHours(), 0.001f, "Remaining hours must clamp at 0.0");
        assertEquals(0, timeManager.getDays(), "Remaining days must be 0");
        assertEquals(0, timeManager.getHours(), "Remaining hours must be 0");
        assertEquals("0d 00h", timeManager.getFormattedTime(), "Formatted string should be 0d 00h");
        assertTrue(timeManager.isExpired(), "Timer should be reported as expired");
    }

    @Test
    @DisplayName("getTime() returns current remaining time correctly across small increments")
    void testGetTimeAccurate() {
        timeManager.update(12.5f);
        assertEquals(1427.5f, timeManager.getTime(), 0.001f);

        timeManager.update(17.5f);
        assertEquals(1410.0f, timeManager.getTime(), 0.001f);
    }
}
