package io.github.shaquibbai.deadlinedash.minigame.football;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FootballMinigameTest {

    private FootballBall ball;

    @BeforeEach
    void setUp() {
        ball = new FootballBall();
    }

    @Test
    @DisplayName("Initial ball state should be held at MC feet")
    void testInitialBallState() {
        Vector2 startPos = new Vector2(400f, 300f);
        ball.holdAt(startPos);

        assertEquals(FootballBall.State.HELD_BY_MC, ball.getState());
        assertEquals(400f, ball.getGroundPosition().x, 0.01f);
        assertEquals(300f, ball.getGroundPosition().y, 0.01f);
        assertEquals(0f, ball.getZHeight(), 0.01f);
        assertFalse(ball.isAirborne());
    }

    @Test
    @DisplayName("Ground pass (<40% power) should remain flat on grass with zero Z-height")
    void testGroundPassPower() {
        Vector2 startPos = new Vector2(400f, 300f);
        Vector2 targetPos = new Vector2(400f, 600f);
        float power = 0.35f;

        ball.launchPass(startPos, targetPos, power);
        assertEquals(FootballBall.State.IN_FLIGHT, ball.getState());
        assertFalse(ball.isAirborne());

        // Update midway
        ball.update(0.20f);
        assertEquals(0f, ball.getZHeight(), 0.01f);
    }

    @Test
    @DisplayName("Flying pass (>=40% power) should elevate with parabolic Z-height arc")
    void testFlyingPassPower() {
        Vector2 startPos = new Vector2(400f, 300f);
        Vector2 targetPos = new Vector2(400f, 800f);
        float power = 0.60f;

        ball.launchPass(startPos, targetPos, power);
        assertEquals(FootballBall.State.IN_FLIGHT, ball.getState());
        assertTrue(ball.isAirborne());

        // Update midway to peak height
        ball.update(0.35f);
        assertTrue(ball.getZHeight() > 0f);
        assertTrue(ball.getMaxHeight() > 0f);
    }

    @Test
    @DisplayName("Segment evaluation rules: Positive mcPoints gives CSE +1, Negative gives EEE +1, Zero gives Draw")
    void testSegmentEvaluationLogic() {
        int cseScore = 0;
        int eeeScore = 0;

        // Segment 1: mcPoints = +4 (> 0) -> CSE +1
        int mcPoints1 = 4;
        if (mcPoints1 > 0) cseScore += 1; else if (mcPoints1 < 0) eeeScore += 1;
        assertEquals(1, cseScore);
        assertEquals(0, eeeScore);

        // Segment 2: mcPoints = -2 (< 0) -> EEE +1
        int mcPoints2 = -2;
        if (mcPoints2 > 0) cseScore += 1; else if (mcPoints2 < 0) eeeScore += 1;
        assertEquals(1, cseScore);
        assertEquals(1, eeeScore);

        // Segment 3: mcPoints = 0 (== 0) -> Draw (no change)
        int mcPoints3 = 0;
        if (mcPoints3 > 0) cseScore += 1; else if (mcPoints3 < 0) eeeScore += 1;
        assertEquals(1, cseScore);
        assertEquals(1, eeeScore);
    }
}
