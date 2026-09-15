package io.github.shaquibbai.deadlinedash.minigame.football;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Entity representing the football in the Football Minigame.
 * Manages ball state, ground vs airborne flight paths, Z-height parabolic arc,
 * dynamic air time based on power/distance, and shadow placement.
 */
public class FootballBall {
    public enum State {
        HELD_BY_MC,
        IN_FLIGHT,
        LANDED
    }

    private final Vector2 currentPosition = new Vector2();
    private final Vector2 previousPosition = new Vector2();
    private final Vector2 startPosition = new Vector2();
    private final Vector2 targetPosition = new Vector2();
    private final Vector2 renderPosition = new Vector2();

    private State state = State.HELD_BY_MC;
    private float flightProgress = 0f;
    private float flightDuration = 0.50f;

    // Power-Based Z-Height Flight Properties
    private float kickPower = 0.5f;
    private boolean isAirborne = false;
    private float maxHeight = 0f;
    private float zHeight = 0f;

    public FootballBall() {
    }

    public void holdAt(Vector2 pos) {
        if (pos != null) {
            this.currentPosition.set(pos);
            this.previousPosition.set(pos);
            this.startPosition.set(pos);
            this.targetPosition.set(pos);
            this.renderPosition.set(pos);
        }
        this.state = State.HELD_BY_MC;
        this.flightProgress = 0f;
        this.zHeight = 0f;
        this.maxHeight = 0f;
        this.isAirborne = false;
    }

    public void launchPass(Vector2 fromPos, Vector2 toPos, float power) {
        if (fromPos != null && toPos != null) {
            this.startPosition.set(fromPos);
            this.previousPosition.set(fromPos);
            this.currentPosition.set(fromPos);
            this.targetPosition.set(toPos);
            this.renderPosition.set(fromPos);
            this.state = State.IN_FLIGHT;
            this.flightProgress = 0f;
            this.kickPower = MathUtils.clamp(power, 0.05f, 1.0f);

            float distance = fromPos.dst(toPos);

            // Flight threshold: power >= 40% (0.40) is AIRBORNE, below 40% is GROUND PASS
            if (this.kickPower < 0.40f) {
                this.isAirborne = false;
                this.maxHeight = 0f;
                this.zHeight = 0f;
                // Quick ground pass flight duration
                this.flightDuration = MathUtils.clamp(0.28f + (distance / 800.0f) * 0.22f, 0.25f, 0.50f);
            } else {
                this.isAirborne = true;
                float flightPower = (this.kickPower - 0.40f) / 0.60f; // Normalized 0.0 to 1.0
                // 40% gives lowest noticeable loft (28px), 100% gives maximum loft (150px)
                this.maxHeight = MathUtils.lerp(28.0f, 150.0f, flightPower);
                this.zHeight = 0f;
                // Airborne passes take longer travel time, increasing interception risk
                this.flightDuration = MathUtils.clamp(0.45f + (distance / 800.0f) * 0.45f, 0.45f, 0.90f);
            }
        }
    }

    // Overload for backward compatibility
    public void launchPass(Vector2 fromPos, Vector2 toPos) {
        launchPass(fromPos, toPos, 0.5f);
    }

    public void update(float delta) {
        if (state == State.IN_FLIGHT) {
            previousPosition.set(currentPosition);

            flightProgress += delta / flightDuration;
            if (flightProgress >= 1.0f) {
                flightProgress = 1.0f;
                currentPosition.set(targetPosition);
                zHeight = 0f;
                state = State.LANDED;
            } else {
                float alpha = MathUtils.clamp(flightProgress, 0f, 1f);
                currentPosition.x = MathUtils.lerp(startPosition.x, targetPosition.x, alpha);
                currentPosition.y = MathUtils.lerp(startPosition.y, targetPosition.y, alpha);

                // Parabolic Arc: z = 4 * maxHeight * t * (1 - t)
                if (isAirborne) {
                    zHeight = 4.0f * maxHeight * alpha * (1.0f - alpha);
                } else {
                    zHeight = 0f;
                }
            }

            renderPosition.set(currentPosition.x, currentPosition.y + zHeight);
        } else {
            zHeight = 0f;
            renderPosition.set(currentPosition);
        }
    }

    public Vector2 getCurrentPosition() {
        return currentPosition;
    }

    public Vector2 getGroundPosition() {
        return currentPosition;
    }

    public Vector2 getRenderPosition() {
        return renderPosition;
    }

    public Vector2 getPreviousPosition() {
        return previousPosition;
    }

    public float getX() {
        return currentPosition.x;
    }

    public float getY() {
        return currentPosition.y;
    }

    public float getZHeight() {
        return zHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public boolean isAirborne() {
        return isAirborne;
    }

    public Vector2 getTargetPosition() {
        return targetPosition;
    }

    public State getState() {
        return state;
    }

    public boolean isInFlight() {
        return state == State.IN_FLIGHT;
    }

    public boolean isLanded() {
        return state == State.LANDED;
    }

    public boolean isHeldByMC() {
        return state == State.HELD_BY_MC;
    }

    public float getFlightProgress() {
        return flightProgress;
    }
}
