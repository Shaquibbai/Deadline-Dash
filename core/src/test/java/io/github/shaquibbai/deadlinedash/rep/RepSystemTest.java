package io.github.shaquibbai.deadlinedash.rep;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepSystemTest {

    private RepSystem repSystem;

    @BeforeEach
    void setUp() {
        repSystem = new RepSystem();
    }

    @Test
    @DisplayName("Starting REP must be exactly 20")
    void testInitialRep() {
        assertEquals(20, repSystem.getRep());
    }

    @Test
    @DisplayName("addRep increases REP value correctly")
    void testAddRep() {
        repSystem.addRep(5);
        assertEquals(25, repSystem.getRep());

        repSystem.addRep(5);
        assertEquals(30, repSystem.getRep());

        repSystem.addRep(10);
        assertEquals(40, repSystem.getRep());
    }

    @Test
    @DisplayName("removeRep decreases REP value and never goes below 0")
    void testRemoveRep() {
        repSystem.removeRep(5);
        assertEquals(15, repSystem.getRep());

        repSystem.removeRep(20);
        assertEquals(0, repSystem.getRep(), "REP should never go below 0");

        repSystem.removeRep(50);
        assertEquals(0, repSystem.getRep());
    }

    @Test
    @DisplayName("Negative add or remove amounts are ignored")
    void testInvalidAmounts() {
        repSystem.addRep(-10);
        assertEquals(20, repSystem.getRep());

        repSystem.removeRep(-10);
        assertEquals(20, repSystem.getRep());
    }

    @Test
    @DisplayName("Reset resets REP to exactly 20")
    void testReset() {
        repSystem.addRep(50);
        assertEquals(70, repSystem.getRep());

        repSystem.reset();
        assertEquals(20, repSystem.getRep());
    }
}
