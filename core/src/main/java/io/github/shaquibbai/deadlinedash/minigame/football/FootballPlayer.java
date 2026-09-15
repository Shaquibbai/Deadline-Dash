package io.github.shaquibbai.deadlinedash.minigame.football;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Entity representing a player on the pitch during the Football Minigame.
 * Tracks position, team role, smooth round transition lerping, and subtle procedural animations.
 */
public class FootballPlayer {
    public enum TeamRole {
        MC,
        CSE_TEAMMATE,
        EEE_OPPONENT,
        GOALKEEPER
    }

    private final Vector2 position = new Vector2();
    private final Vector2 startPosition = new Vector2();
    private final Vector2 targetPosition = new Vector2();
    private TeamRole role;
    private float radius = 22f; // Player visual radius in world units

    // Round Transition Lerp State
    private boolean isTransitioning = false;
    private float transitionProgress = 0f;
    private static final float TRANSITION_DURATION = 0.40f; // Smooth movement to new round position

    // Animation Timers
    private float kickAnimTimer = 0f;      // Brief MC kick preparation (0.12s)
    private float reactionAnimTimer = 0f;  // Brief reception / interception reaction (0.20s)
    private float idlePhase = MathUtils.random(0f, MathUtils.PI2);

    // Consistent Team Colors
    public static final Color COLOR_CSE = new Color(0.20f, 0.65f, 0.95f, 1.0f);   // Sky Blue
    public static final Color COLOR_EEE = new Color(0.98f, 0.85f, 0.10f, 1.0f);   // Yellow
    public static final Color COLOR_GK  = new Color(0.95f, 0.40f, 0.10f, 1.0f);   // Orange / Neutral Contrast
    public static final Color COLOR_MC_HIGHLIGHT = new Color(1.0f, 0.85f, 0.20f, 1.0f); // Bright Gold Badge

    public FootballPlayer(float x, float y, TeamRole role) {
        this.position.set(x, y);
        this.startPosition.set(x, y);
        this.targetPosition.set(x, y);
        this.role = role;
    }

    public void startRoundTransition(float targetX, float targetY) {
        this.startPosition.set(position);
        this.targetPosition.set(targetX, targetY);
        this.isTransitioning = true;
        this.transitionProgress = 0f;
    }

    public void update(float delta) {
        // 1. Update smooth round redistribution movement lerp
        if (isTransitioning) {
            transitionProgress += delta / TRANSITION_DURATION;
            if (transitionProgress >= 1.0f) {
                transitionProgress = 1.0f;
                position.set(targetPosition);
                isTransitioning = false;
            } else {
                float alpha = MathUtils.clamp(transitionProgress, 0f, 1f);
                // Smooth step curve for natural player movement
                float smoothAlpha = alpha * alpha * (3 - 2 * alpha);
                position.x = MathUtils.lerp(startPosition.x, targetPosition.x, smoothAlpha);
                position.y = MathUtils.lerp(startPosition.y, targetPosition.y, smoothAlpha);
            }
        }

        // 2. Update animation timers
        if (kickAnimTimer > 0f) {
            kickAnimTimer -= delta;
            if (kickAnimTimer < 0f) kickAnimTimer = 0f;
        }

        if (reactionAnimTimer > 0f) {
            reactionAnimTimer -= delta;
            if (reactionAnimTimer < 0f) reactionAnimTimer = 0f;
        }
    }

    public void triggerKickAnimation() {
        this.kickAnimTimer = 0.12f;
    }

    public void triggerReactionAnimation() {
        this.reactionAnimTimer = 0.20f;
    }

    public boolean isKickAnimating() {
        return kickAnimTimer > 0f;
    }

    public boolean isReactionAnimating() {
        return reactionAnimTimer > 0f;
    }

    public float getKickAnimProgress() {
        return kickAnimTimer / 0.12f;
    }

    public float getReactionAnimProgress() {
        return reactionAnimTimer / 0.20f;
    }

    public float getIdleBobbingY(float animTime) {
        if (isTransitioning || isKickAnimating()) return 0f;
        return (float) Math.sin(animTime * 3.5f + idlePhase) * 1.5f;
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public void setPosition(float x, float y) {
        this.position.set(x, y);
        this.startPosition.set(x, y);
        this.targetPosition.set(x, y);
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isMC() {
        return role == TeamRole.MC;
    }

    public boolean isCseTeammate() {
        return role == TeamRole.CSE_TEAMMATE;
    }

    public boolean isEeeOpponent() {
        return role == TeamRole.EEE_OPPONENT;
    }

    public boolean isGoalkeeper() {
        return role == TeamRole.GOALKEEPER;
    }

    public boolean isPassableTeammate() {
        return role == TeamRole.CSE_TEAMMATE; // MC passes to CSE teammates
    }

    /**
     * Returns the primary kit color for rendering this player.
     */
    public Color getKitColor() {
        switch (role) {
            case MC:
            case CSE_TEAMMATE:
                return COLOR_CSE;
            case EEE_OPPONENT:
                return COLOR_EEE;
            case GOALKEEPER:
                return COLOR_GK;
            default:
                return Color.WHITE;
        }
    }
}
