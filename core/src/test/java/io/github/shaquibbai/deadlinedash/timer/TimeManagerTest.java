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
    @DisplayName("Initial game time must equal 36 hours (1080 real-time seconds = 18 real minutes)")
    void testInitialTime() {
        assertEquals(1080.0f, timeManager.getTime(), 0.001f, "Initial time returned by getTime() must be 1080.0 seconds");
        assertEquals(36.0f, timeManager.getRemainingHours(), 0.001f, "Initial remaining hours must be 36.0");
        assertEquals(1, timeManager.getDays(), "Initial remaining days must be 1");
        assertEquals(12, timeManager.getHours(), "Initial remaining hours component must be 12");
        assertEquals(36, timeManager.getDisplayHours(), "Initial display hours must be 36");
        assertEquals(0, timeManager.getDisplayMinutes(), "Initial display minutes must be 0");
        assertEquals("36 : 00", timeManager.getFormattedTime(), "Initial formatted time must be '36 : 00'");
    }

    @Test
    @DisplayName("Timer displays 15 game-minute stepped sequence: 36:00 -> 35:45 -> 35:30 -> 35:15 -> 35:00")
    void testFifteenMinuteSteppedSequence() {
        // At start: 36 : 00
        assertEquals("36 : 00", timeManager.getFormattedTime());

        // 5 real seconds elapsed (10 game minutes elapsed) -> still within first 15m block, displays 36 : 00
        timeManager.update(5.0f);
        assertEquals(1075.0f, timeManager.getTime(), 0.001f);
        assertEquals("36 : 00", timeManager.getFormattedTime(), "During first 15 game minutes, clock stays at 36 : 00");

        // 2.5 more real seconds elapsed (total 7.5s = 15 game minutes elapsed) -> steps to 35 : 45
        timeManager.update(2.5f);
        assertEquals(1072.5f, timeManager.getTime(), 0.001f);
        assertEquals("35 : 45", timeManager.getFormattedTime(), "After 7.5s (15 game minutes), clock steps to 35 : 45");

        // Another 7.5s elapsed (total 15.0s = 30 game minutes elapsed) -> steps to 35 : 30
        timeManager.update(7.5f);
        assertEquals(1065.0f, timeManager.getTime(), 0.001f);
        assertEquals("35 : 30", timeManager.getFormattedTime(), "After 15.0s (30 game minutes), clock steps to 35 : 30");

        // Another 7.5s elapsed (total 22.5s = 45 game minutes elapsed) -> steps to 35 : 15
        timeManager.update(7.5f);
        assertEquals(1057.5f, timeManager.getTime(), 0.001f);
        assertEquals("35 : 15", timeManager.getFormattedTime(), "After 22.5s (45 game minutes), clock steps to 35 : 15");

        // Another 7.5s elapsed (total 30.0s = 1 game hour elapsed) -> steps to 35 : 00
        timeManager.update(7.5f);
        assertEquals(1050.0f, timeManager.getTime(), 0.001f);
        assertEquals("35 : 00", timeManager.getFormattedTime(), "After 30.0s (1 game hour), clock steps to 35 : 00");
    }

    @Test
    @DisplayName("Underlying timer counts down continuously while visual clock updates discretely")
    void testUnderlyingTimerContinuity() {
        // Continuous updates of 1.0 real second
        for (int i = 1; i <= 7; i++) {
            timeManager.update(1.0f);
            assertEquals(1080.0f - i, timeManager.getTime(), 0.001f, "Underlying timer must count down continuously");
            assertEquals("36 : 00", timeManager.getFormattedTime(), "Visual clock must remain 36 : 00 until 7.5s mark");
        }

        timeManager.update(0.5f); // Now reaches 7.5s
        assertEquals(1072.5f, timeManager.getTime(), 0.001f);
        assertEquals("35 : 45", timeManager.getFormattedTime());
    }

    @Test
    @DisplayName("Timer clamps at zero and displays 00 : 00 upon exact expiration")
    void testTimerClampingAtZero() {
        timeManager.update(2000.0f);

        assertEquals(0.0f, timeManager.getTime(), 0.001f, "Timer must clamp at 0.0 seconds");
        assertEquals(0.0f, timeManager.getRemainingHours(), 0.001f, "Remaining hours must clamp at 0.0");
        assertEquals(0, timeManager.getDays(), "Remaining days must be 0");
        assertEquals(0, timeManager.getHours(), "Remaining hours must be 0");
        assertEquals(0, timeManager.getDisplayHours());
        assertEquals(0, timeManager.getDisplayMinutes());
        assertEquals("00 : 00", timeManager.getFormattedTime(), "Formatted string should be 00 : 00 at zero");
        assertTrue(timeManager.isExpired(), "Timer should be reported as expired");
    }

    @Test
    @DisplayName("getTime() returns current remaining time accurately")
    void testGetTimeAccurate() {
        timeManager.update(12.5f);
        assertEquals(1067.5f, timeManager.getTime(), 0.001f);

        timeManager.update(17.5f);
        assertEquals(1050.0f, timeManager.getTime(), 0.001f);
    }
}