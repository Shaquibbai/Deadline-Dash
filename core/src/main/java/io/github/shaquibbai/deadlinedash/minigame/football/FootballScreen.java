package io.github.shaquibbai.deadlinedash.minigame.football;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.shaquibbai.deadlinedash.DeadlineDash;
import io.github.shaquibbai.deadlinedash.screen.GameScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Standalone Football Minigame Screen for Deadline Dash.
 * Features Power-Based Ball Flight (Ground Pass <40%, Flying Pass >=40%), Z-axis height arc,
 * dynamic air time based on power/distance, subtle ground shadow, 40% airborne power threshold marker,
 * enlarged VERTICAL pitch (960x860), zoomed-out camera viewport (1600x1000), and screen-anchored UI.
 * Isolated from quest progression and dialogues, launched via debug key 'T'.
 */
public class FootballScreen implements Screen {
    public interface FootballResultListener {
        void onMatchCompleted(int cseScore, int eeeScore);
    }

    private final DeadlineDash game;
    private final GameScreen previousScreen;
    private final SpriteBatch batch;
    private FootballResultListener resultListener;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture whitePixel;
    private BitmapFont titleFont;
    private BitmapFont hudFont;
    private BitmapFont toastFont;

    private final GlyphLayout layout = new GlyphLayout();

    // Viewport & Camera Dimensions
    private static final float VIEW_W = 1600f;
    private static final float VIEW_H = 1000f;

    // Field Dimensions (Enlarged Vertical Half Pitch in 1600x1000 Viewport)
    private static final float FIELD_W = 960f;
    private static final float FIELD_H = 860f;
    private static final float FIELD_X = (VIEW_W - FIELD_W) / 2f; // 320f (Centered horizontally)
    private static final float FIELD_Y = 60f;                      // 60f to 920f vertically

    // Vertical Pitch Markings (Goal at the TOP of the screen)
    private static final float GOAL_LINE_Y = FIELD_Y + FIELD_H;   // 920f (Top boundary line)
    private static final float BOTTOM_LINE_Y = FIELD_Y;           // 60f (Half-way line at bottom)
    private static final float GOAL_W = 200f;
    private static final float GOAL_X = FIELD_X + (FIELD_W - GOAL_W) / 2f; // 700f (Centered)

    private static final float GOAL_BOX_W = 260f;
    private static final float GOAL_BOX_H = 90f;
    private static final float PENALTY_BOX_W = 480f;
    private static final float PENALTY_BOX_H = 240f;

    // Gameplay Parameters
    private static final float RECEIVING_RADIUS = 28.0f;         // Hidden internal receiving radius
    private static final float INTERCEPTION_HEIGHT = 35.0f;      // Max ball zHeight for airborne interception
    private static final float MAX_KICK_DISTANCE = 780.0f;       // Tuned max kick distance for enlarged pitch
    private static final float ROUND_DURATION = 5.0f;           // 5 seconds decision timer
    private static final float TOTAL_MATCH_DURATION = 150.0f;    // 2.5 minutes total match time
    private static final float MIN_PLAYER_SEPARATION = 95.0f;    // Rebalanced min spacing on larger pitch
    private static final float MAX_CHASE_DISTANCE = 220.0f;     // Max distance for nearest player to chase landed ball
    private static final float CHASE_SPEED = 240.0f;           // Player chase movement speed in px/s
    private static final float ARRIVAL_DISTANCE = 12.0f;        // Arrival distance threshold to capture landed ball

    private static final float INTRO_PASS_DURATION = 1.00f;     // Duration of opening scripted pass to MC in seconds

    // Pitch Players & Ball
    private final List<FootballPlayer> players = new ArrayList<>();
    private FootballPlayer mcPlayer;
    private FootballPlayer goalkeeper;
    private FootballPlayer chasingPlayer = null;
    private final FootballBall ball = new FootballBall();

    // Aim & Power State
    private float aimAngle = MathUtils.PI / 2f; // Radians (PI/2 = facing UP towards top goal)
    private float kickPower = 0.5f;             // Clamped [0.05..1.0]

    // Timers & Score Tracking
    private float matchTimer = TOTAL_MATCH_DURATION;
    private float roundTimer = ROUND_DURATION;
    private float introPassTimer = 0f;
    private float roundResultTimer = 0f;
    private int mcPoints = 0;
    private int cseScore = 0;
    private int eeeScore = 0;
    private int evaluatedSegments = 0;
    private int successfulPasses = 0;
    private int failedPasses = 0;
    private int totalRounds = 0;

    // Screen State
    public enum GameState {
        MATCH_INTRO_PASS,
        AIMING,
        MC_KICKING,
        BALL_IN_FLIGHT,
        BALL_GROUND_RECOVERY,
        ROUND_RESULT,
        MATCH_OVER
    }

    public enum CommentaryType {
        SUCCESS,
        FAIL,
        TIMEOUT
    }

    private static class CommentaryItem {
        final String text;
        final Color color;

        CommentaryItem(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private static class CommentaryGroup {
        final List<CommentaryItem> items;

        CommentaryGroup(List<CommentaryItem> items) {
            this.items = items;
        }
    }

    // Sideline Commentary System (Left-side shout bubbles burst)
    private static final String[] SUCCESS_COMMENTS = {
        "That's how you do it...",
        "That's the cleanest pass of history",
        "How come i've never seen him before!!",
        "Oh!! What a pass!!!",
        "Bro finally woke up!",
        "THAT WAS ACTUALLY GOOD!!!",
        "Keep going, CSE!",
        "WHO IS THIS GUY??",
        "Okay okay, I see you!",
        "THAT'S THE STUFF!!!",
        "BRO HAS FINALLY LOCKED IN!!!",
        "WHO GAVE HIM THE CONTROLLER??",
        "WAIT... THAT WAS GOOD??",
        "CSE IS ALIVE!!!"
    };

    private static final String[] FAIL_COMMENTS = {
        "Look where you pass, you dimwit!!!",
        "Get this stupid subbed!",
        "How much you took from them??",
        "I bet he's wearing yellow inside!!",
        "Get your eyes fixed",
        "BRO WHO TAUGHT YOU FOOTBALL??",
        "PASS THE BALL, NOT YOUR FUTURE!!!",
        "THAT BALL HAS MORE IQ THAN YOU!!!",
        "WHO ARE YOU PASSING TO?? YOUR ANCESTORS??",
        "REF, CHECK HIS BRAIN!!!",
        "HE'S PLAYING WITH WIFI DELAY!!!",
        "WHY DID YOU PASS THERE?!",
        "SOMEONE TAKE HIS SHOES!!!",
        "BRO THINKS THIS IS FIFA!!!",
        "MY GRANDMA COULD DEFEND THAT!!!",
        "HE SAW THE BALL AND PANICKED!!!",
        "THAT PASS HAD NO DESTINATION!!!",
        "BRO JUST DONATED THE BALL!!!",
        "EEE THANKS FOR THE GIFT!!!",
        "WHAT WAS THE PLAN THERE??",
        "HE PASSED TO THE ENEMY!!!"
    };

    private static final String[] TIMEOUT_COMMENTS = {
        "What are you waiting for, stupid?? PASS!",
        "Didn't have breakfast huh??",
        "BRO, THE CLOCK EXISTS!!!",
        "PASS BEFORE GRADUATION!!!",
        "WHAT ARE YOU WAITING FOR??",
        "HE'S THINKING ABOUT HIS LIFE AGAIN!!!",
        "5 SECONDS AND NO PASS???",
        "BRO FORGOT HOW TIME WORKS!!!",
        "THE BALL IS NOT GOING TO PASS ITSELF!!!",
        "WAKE UP!!!"
    };

    private CommentaryGroup activeGroup = null;
    private CommentaryGroup previousGroup = null;
    private static final float CROSSFADE_DURATION = 0.40f; // 0.4s smooth fade transition
    private float crossfadeTimer = 0f;
    private String lastGroupFirstComment = null;

    private GameState state = GameState.AIMING;
    private float kickPrepTimer = 0f;
    private Vector2 pendingTargetPos = new Vector2();
    private float animTime = 0f;

    public FootballScreen(DeadlineDash game, GameScreen previousScreen) {
        this(game, previousScreen, null);
    }

    public FootballScreen(DeadlineDash game, GameScreen previousScreen, FootballResultListener resultListener) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.resultListener = resultListener;
        this.batch = game.getBatch();

        initViewport();
        initAssets();
        initMatch();
    }

    public void setResultListener(FootballResultListener resultListener) {
        this.resultListener = resultListener;
    }

    private void initViewport() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply();
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
        camera.update();
    }

    private void initAssets() {
        // Single Pixel Texture for UI, Pitch lines, and Silhouettes
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        // Bitmap Fonts initialized with crisp scaling for 1600x1000 viewport
        titleFont = new BitmapFont();
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        titleFont.getData().setScale(2.0f);

        hudFont = new BitmapFont();
        hudFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        hudFont.getData().setScale(1.25f);

        toastFont = new BitmapFont();
        toastFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        toastFont.getData().setScale(2.4f);
    }

    private void initMatch() {
        matchTimer = TOTAL_MATCH_DURATION;
        mcPoints = 0;
        cseScore = 0;
        eeeScore = 0;
        evaluatedSegments = 0;
        successfulPasses = 0;
        failedPasses = 0;
        activeGroup = null;
        previousGroup = null;
        crossfadeTimer = 0f;
        lastGroupFirstComment = null;

        // Fixed Goalkeeper near top goal line center
        goalkeeper = new FootballPlayer(VIEW_W / 2f, GOAL_LINE_Y - 30f, FootballPlayer.TeamRole.GOALKEEPER);

        startNextRound();
        triggerOpeningPass();
    }

    public int getMcPoints() {
        return mcPoints;
    }

    public int getCseScore() {
        return cseScore;
    }

    public int getEeeScore() {
        return eeeScore;
    }

    private int getScriptedCseScore() {
        float elapsed = TOTAL_MATCH_DURATION - matchTimer;
        if (elapsed < 120.0f) {
            return 0;
        } else if (elapsed < 147.0f) {
            return 1;
        } else {
            return 2;
        }
    }

    private int getScriptedEeeScore() {
        float elapsed = TOTAL_MATCH_DURATION - matchTimer;
        if (elapsed < 30.0f) {
            return 0;
        } else {
            return 1;
        }
    }

    private void triggerOpeningPass() {
        Vector2 mcPos = mcPlayer.getPosition();
        Vector2 startPos = new Vector2(FIELD_X + (FIELD_W / 2.0f), GOAL_LINE_Y - 180.0f);

        // Scripted opening ground pass from top half to MC feet over 1.00s
        ball.launchPass(startPos, mcPos, 0.20f);
        introPassTimer = INTRO_PASS_DURATION;
        state = GameState.MATCH_INTRO_PASS;
    }

    /**
     * Smoothly redistributes all non-goalkeeper players across the pitch for a new round.
     */
    private void startNextRound() {
        totalRounds++;
        boolean isFirstRound = players.isEmpty();

        if (isFirstRound) {
            players.add(goalkeeper);

            // 1. Place MC at valid position in bottom build-up region
            float mcX = MathUtils.random(FIELD_X + 120f, FIELD_X + FIELD_W - 120f);
            float mcY = MathUtils.random(FIELD_Y + 70f, FIELD_Y + 240f);
            mcPlayer = new FootballPlayer(mcX, mcY, FootballPlayer.TeamRole.MC);
            players.add(mcPlayer);

            // 2. Place 4-5 CSE Teammates (Total CSE = MC + 4 or 5 = 5 or 6)
            int numTeammates = MathUtils.random(4, 5);
            for (int i = 0; i < numTeammates; i++) {
                Vector2 pos = generateValidPlayerPos(FIELD_Y + 220f, GOAL_LINE_Y - 110f);
                players.add(new FootballPlayer(pos.x, pos.y, FootballPlayer.TeamRole.CSE_TEAMMATE));
            }

            // 3. Place 6-8 EEE Opponents (Total EEE = 6, 7, or 8)
            int numOpponents = MathUtils.random(6, 8);
            for (int i = 0; i < numOpponents; i++) {
                Vector2 pos = generateValidPlayerPos(FIELD_Y + 240f, GOAL_LINE_Y - 80f);
                players.add(new FootballPlayer(pos.x, pos.y, FootballPlayer.TeamRole.EEE_OPPONENT));
            }
        } else {
            // Smoothly move existing players to new random positions
            float newMcX = MathUtils.random(FIELD_X + 120f, FIELD_X + FIELD_W - 120f);
            float newMcY = MathUtils.random(FIELD_Y + 70f, FIELD_Y + 240f);
            mcPlayer.startRoundTransition(newMcX, newMcY);

            for (FootballPlayer p : players) {
                if (p.isMC() || p.isGoalkeeper()) continue;
                if (p.isCseTeammate()) {
                    Vector2 pos = generateValidPlayerPos(FIELD_Y + 220f, GOAL_LINE_Y - 110f);
                    p.startRoundTransition(pos.x, pos.y);
                } else if (p.isEeeOpponent()) {
                    Vector2 pos = generateValidPlayerPos(FIELD_Y + 240f, GOAL_LINE_Y - 80f);
                    p.startRoundTransition(pos.x, pos.y);
                }
            }
        }

        // Reset ball at MC position
        chasingPlayer = null;
        ball.holdAt(mcPlayer.getPosition());

        // Default aim facing UP towards the goal
        aimAngle = MathUtils.PI / 2f;
        kickPower = 0.5f;

        roundTimer = ROUND_DURATION;
        state = GameState.AIMING;
    }

    /**
     * Generates a random position maintaining minimum separation distance (95px) from all existing players.
     */
    private Vector2 generateValidPlayerPos(float minY, float maxY) {
        float minX = FIELD_X + 80f;
        float maxX = FIELD_X + FIELD_W - 80f;
        float minSeparationSq = MIN_PLAYER_SEPARATION * MIN_PLAYER_SEPARATION;

        for (int attempt = 0; attempt < 150; attempt++) {
            float candidateX = MathUtils.random(minX, maxX);
            float candidateY = MathUtils.random(minY, maxY);
            boolean valid = true;

            for (FootballPlayer existing : players) {
                float dx = candidateX - existing.getX();
                float dy = candidateY - existing.getY();
                if ((dx * dx + dy * dy) < minSeparationSq) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return new Vector2(candidateX, candidateY);
            }
        }
        // Fallback position if placement search loop times out
        return new Vector2(MathUtils.random(minX, maxX), MathUtils.random(minY, maxY));
    }

    @Override
    public void render(float delta) {
        animTime += delta;
        ScreenUtils.clear(0.06f, 0.10f, 0.06f, 1.0f);

        // Update timers, player transitions, ball flight, and real-time path interception
        updateGameLogic(delta);

        // Render Pitch, Human Players, Ball with Shadow/Z-Height, Aim Direction Ray, and UI Overlay
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawStylizedPitch();
        drawHumanPlayers();
        drawBall();

        if (state == GameState.AIMING || state == GameState.MC_KICKING) {
            drawAimDirectionRay();
        }

        drawHUD();

        if (activeGroup != null) {
            drawSidelineCommentary();
        }

        if (state == GameState.MATCH_OVER) {
            drawMatchOverModal();
        }

        batch.end();

        // Process Input
        handleInput(delta);
    }

    private void updateGameLogic(float delta) {
        // Always update sideline commentary burst queue
        updateCommentary(delta);

        // Update player transitions & procedural animations
        for (FootballPlayer p : players) {
            p.update(delta);
        }

        if (state == GameState.MATCH_OVER) {
            return;
        }

        if (state == GameState.MATCH_INTRO_PASS) {
            // Scripted opening pass to MC feet over 1.00s
            introPassTimer -= delta;
            ball.update(delta);

            if (introPassTimer <= 0f || ball.isLanded()) {
                ball.holdAt(mcPlayer.getPosition());
                roundTimer = ROUND_DURATION;
                state = GameState.AIMING;
            }
            return;
        }

        // Overall match timer countdown
        matchTimer -= delta;
        if (matchTimer <= 0f) {
            matchTimer = 0f;
            checkSegmentEvaluations(TOTAL_MATCH_DURATION);
            state = GameState.MATCH_OVER;
            return;
        }

        // Check 40-Second Segment Evaluations (at 40s, 80s, 120s elapsed)
        float elapsedMatchTime = TOTAL_MATCH_DURATION - matchTimer;
        checkSegmentEvaluations(elapsedMatchTime);

        if (state == GameState.AIMING) {
            // Keep ball anchored at MC feet while aiming
            ball.holdAt(mcPlayer.getPosition());

            // Round decision timer countdown
            roundTimer -= delta;
            if (roundTimer <= 0f) {
                roundTimer = 0f;
                handleTimeout();
            }
        } else if (state == GameState.MC_KICKING) {
            // MC kick preparation animation (0.12s) before ball launches
            kickPrepTimer -= delta;
            ball.holdAt(mcPlayer.getPosition());
            if (kickPrepTimer <= 0f) {
                ball.launchPass(mcPlayer.getPosition(), pendingTargetPos, kickPower);
                state = GameState.BALL_IN_FLIGHT;
            }
        } else if (state == GameState.BALL_IN_FLIGHT) {
            ball.update(delta);

            // REAL-TIME PATH INTERCEPTION: First contact wins along travelling path when ball is low enough!
            Vector2 prevPos = ball.getPreviousPosition();
            Vector2 currPos = ball.getCurrentPosition();
            float currentZ = ball.getZHeight();
            boolean isAirbornePass = ball.isAirborne();

            FootballPlayer firstHitPlayer = null;
            float earliestT = Float.MAX_VALUE;

            for (FootballPlayer p : players) {
                if (p.isMC()) continue;

                // Check segment distance from previous frame ball position to current frame ball position
                float dist = distanceSegmentToPoint(prevPos, currPos, p.getPosition());
                if (dist <= RECEIVING_RADIUS) {
                    // Height-Aware Interception Rule:
                    // Ground pass (not airborne) OR ball current zHeight <= INTERCEPTION_HEIGHT
                    boolean canIntercept = (!isAirbornePass) || (currentZ <= INTERCEPTION_HEIGHT);
                    if (canIntercept) {
                        float t = getSegmentProjectionT(prevPos, currPos, p.getPosition());
                        if (t < earliestT) {
                            earliestT = t;
                            firstHitPlayer = p;
                        }
                    }
                }
            }

            if (firstHitPlayer != null) {
                // Ball touched a player's receiving radius! First contact wins immediately.
                chasingPlayer = null;
                ball.holdAt(firstHitPlayer.getPosition());
                firstHitPlayer.triggerReactionAnimation();

                if (firstHitPlayer.isPassableTeammate()) {
                    mcPoints += 1;
                    successfulPasses++;
                    triggerCommentaryBurst(CommentaryType.SUCCESS, new Color(0.30f, 0.95f, 0.40f, 1.0f));
                } else {
                    mcPoints -= 1;
                    failedPasses++;
                    triggerCommentaryBurst(CommentaryType.FAIL, new Color(1.0f, 0.35f, 0.35f, 1.0f));
                }
                roundResultTimer = 0.40f;
                state = GameState.ROUND_RESULT;
            } else if (ball.isLanded()) {
                // Ball completed flight to landing point without mid-flight interception.
                // GROUND BALL RECOVERY: Find single nearest outfield player to ball ground position.
                FootballPlayer nearest = null;
                float minDst = Float.MAX_VALUE;
                Vector2 ballGroundPos = ball.getGroundPosition();

                for (FootballPlayer p : players) {
                    if (p.isMC()) continue;
                    float d = p.getPosition().dst(ballGroundPos);
                    if (d < minDst) {
                        minDst = d;
                        nearest = p;
                    }
                }

                if (nearest != null && minDst <= MAX_CHASE_DISTANCE) {
                    // Nearest player is within MAX_CHASE_DISTANCE (220px) -> chase ball!
                    chasingPlayer = nearest;
                    state = GameState.BALL_GROUND_RECOVERY;
                } else {
                    // Nobody chases (all > MAX_CHASE_DISTANCE)
                    chasingPlayer = null;
                    mcPoints -= 1;
                    failedPasses++;
                    triggerCommentaryBurst(CommentaryType.FAIL, new Color(1.0f, 0.65f, 0.20f, 1.0f));
                    roundResultTimer = 0.40f;
                    state = GameState.ROUND_RESULT;
                }
            }
        } else if (state == GameState.BALL_GROUND_RECOVERY) {
            // SINGLE NEAREST PLAYER CHASES LANDED BALL
            if (chasingPlayer != null) {
                Vector2 ballGroundPos = ball.getGroundPosition();
                chasingPlayer.moveTowards(ballGroundPos, CHASE_SPEED, delta);

                float dist = chasingPlayer.getPosition().dst(ballGroundPos);
                if (dist <= ARRIVAL_DISTANCE) {
                    // Chasing player reaches the ball and captures it!
                    ball.holdAt(chasingPlayer.getPosition());
                    chasingPlayer.triggerReactionAnimation();

                    if (chasingPlayer.isPassableTeammate()) {
                        mcPoints += 1;
                        successfulPasses++;
                        triggerCommentaryBurst(CommentaryType.SUCCESS, new Color(0.30f, 0.95f, 0.40f, 1.0f));
                    } else {
                        mcPoints -= 1;
                        failedPasses++;
                        triggerCommentaryBurst(CommentaryType.FAIL, new Color(1.0f, 0.35f, 0.35f, 1.0f));
                    }
                    chasingPlayer = null;
                    roundResultTimer = 0.40f;
                    state = GameState.ROUND_RESULT;
                }
            } else {
                roundResultTimer = 0.40f;
                state = GameState.ROUND_RESULT;
            }
        } else if (state == GameState.ROUND_RESULT) {
            roundResultTimer -= delta;
            if (roundResultTimer <= 0f) {
                startNextRound();
            }
        }
    }

    private void checkSegmentEvaluations(float elapsedMatchTime) {
        if (evaluatedSegments == 0 && elapsedMatchTime >= 40.0f) {
            evaluateSegmentScore();
            evaluatedSegments = 1;
        } else if (evaluatedSegments == 1 && elapsedMatchTime >= 80.0f) {
            evaluateSegmentScore();
            evaluatedSegments = 2;
        } else if (evaluatedSegments == 2 && elapsedMatchTime >= 120.0f) {
            evaluateSegmentScore();
            evaluatedSegments = 3;
        } else if (evaluatedSegments == 3 && elapsedMatchTime >= 150.0f) {
            evaluateSegmentScore();
            evaluatedSegments = 4;
        }
    }

    private void evaluateSegmentScore() {
        if (mcPoints > 0) {
            cseScore += 1;
        } else if (mcPoints < 0) {
            eeeScore += 1;
        }
        // Reset MC performance points for the next 40-second segment
        mcPoints = 0;
    }

    private float distanceSegmentToPoint(Vector2 segStart, Vector2 segEnd, Vector2 pt) {
        float dx = segEnd.x - segStart.x;
        float dy = segEnd.y - segStart.y;
        float lenSq = dx * dx + dy * dy;
        if (lenSq == 0f) return segStart.dst(pt);
        float t = MathUtils.clamp(((pt.x - segStart.x) * dx + (pt.y - segStart.y) * dy) / lenSq, 0f, 1f);
        float projX = segStart.x + t * dx;
        float projY = segStart.y + t * dy;
        float pdx = pt.x - projX;
        float pdy = pt.y - projY;
        return (float) Math.sqrt(pdx * pdx + pdy * pdy);
    }

    private float getSegmentProjectionT(Vector2 segStart, Vector2 segEnd, Vector2 pt) {
        float dx = segEnd.x - segStart.x;
        float dy = segEnd.y - segStart.y;
        float lenSq = dx * dx + dy * dy;
        if (lenSq == 0f) return 0f;
        return MathUtils.clamp(((pt.x - segStart.x) * dx + (pt.y - segStart.y) * dy) / lenSq, 0f, 1f);
    }

    private void handleInput(float delta) {
        // Quick ESC exit check
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            returnToGameScreen();
            return;
        }

        if (state == GameState.MATCH_OVER) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                returnToGameScreen();
            }
            return;
        }

        if (state != GameState.AIMING) {
            return;
        }

        // Aim Controls (A / LEFT -> rotate left, D / RIGHT -> rotate right)
        float aimSpeed = 2.2f; // Radians per second
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            aimAngle += aimSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            aimAngle -= aimSpeed * delta;
        }
        // Normalize angle within [-PI, PI]
        while (aimAngle > MathUtils.PI) aimAngle -= MathUtils.PI2;
        while (aimAngle < -MathUtils.PI) aimAngle += MathUtils.PI2;

        // Kick Power Controls (W / UP -> increase power, S / DOWN -> decrease power)
        float powerSpeed = 0.95f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            kickPower += powerSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            kickPower -= powerSpeed * delta;
        }
        kickPower = MathUtils.clamp(kickPower, 0.05f, 1.0f);

        // SPACE -> Kick Ball
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            executeKick();
        }
    }

    private void executeKick() {
        // Power curve: distance = MAX_KICK_DISTANCE * power^1.20
        float effectivePowerRatio = MathUtils.clamp((float) Math.pow(kickPower, 1.20), 0.02f, 1.0f);
        float kickDistance = MAX_KICK_DISTANCE * effectivePowerRatio;

        float landingX = mcPlayer.getX() + MathUtils.cos(aimAngle) * kickDistance;
        float landingY = mcPlayer.getY() + MathUtils.sin(aimAngle) * kickDistance;

        // Clamp landing position within vertical pitch boundaries
        landingX = MathUtils.clamp(landingX, FIELD_X + 20f, FIELD_X + FIELD_W - 20f);
        landingY = MathUtils.clamp(landingY, FIELD_Y + 20f, GOAL_LINE_Y - 10f);

        pendingTargetPos.set(landingX, landingY);

        // Trigger MC kick preparation animation before launching ball
        mcPlayer.triggerKickAnimation();
        kickPrepTimer = 0.12f;
        state = GameState.MC_KICKING;
    }

    private void updateCommentary(float delta) {
        if (crossfadeTimer > 0f) {
            crossfadeTimer -= delta;
            if (crossfadeTimer <= 0f) {
                crossfadeTimer = 0f;
                previousGroup = null;
            }
        }
    }

    private void triggerCommentaryBurst(CommentaryType type, Color color) {
        String[] pool;
        switch (type) {
            case SUCCESS:
                pool = SUCCESS_COMMENTS;
                break;
            case FAIL:
                pool = FAIL_COMMENTS;
                break;
            case TIMEOUT:
            default:
                pool = TIMEOUT_COMMENTS;
                break;
        }

        int count = MathUtils.random(3, 4);
        List<String> available = new ArrayList<>(Arrays.asList(pool));
        Collections.shuffle(available);

        if (lastGroupFirstComment != null && available.size() > 1 && available.get(0).equals(lastGroupFirstComment)) {
            Collections.swap(available, 0, 1);
        }

        lastGroupFirstComment = available.get(0);

        List<CommentaryItem> items = new ArrayList<>();
        for (int i = 0; i < Math.min(count, available.size()); i++) {
            items.add(new CommentaryItem(available.get(i), color));
        }

        CommentaryGroup group = new CommentaryGroup(items);
        previousGroup = activeGroup;
        activeGroup = group;
        crossfadeTimer = CROSSFADE_DURATION;
    }

    private void handleTimeout() {
        mcPoints -= 1;
        failedPasses++;
        triggerCommentaryBurst(CommentaryType.TIMEOUT, new Color(1.0f, 0.70f, 0.20f, 1.0f));
        roundResultTimer = 0.40f;
        state = GameState.ROUND_RESULT;
    }

    // =========================================================================
    // RENDERING HELPERS
    // =========================================================================

    /**
     * Renders a clean, stylized football pitch with complete markings, goal structure, and grass depth.
     */
    private void drawStylizedPitch() {
        // 1. Dark Surround Boundary
        batch.setColor(0.10f, 0.25f, 0.12f, 1.0f);
        batch.draw(whitePixel, FIELD_X - 20f, FIELD_Y - 20f, FIELD_W + 40f, FIELD_H + 40f);

        // 2. Alternating Horizontal Grass Stripes
        int numStripes = 10;
        float stripeH = FIELD_H / (float) numStripes;
        for (int i = 0; i < numStripes; i++) {
            if (i % 2 == 0) {
                batch.setColor(0.17f, 0.52f, 0.21f, 1.0f);
            } else {
                batch.setColor(0.21f, 0.58f, 0.25f, 1.0f);
            }
            batch.draw(whitePixel, FIELD_X, FIELD_Y + (i * stripeH), FIELD_W, stripeH);
        }

        // 3. Crisp Off-White Pitch Markings (3px border & halfway line, 2px interior boxes)
        Color lineCol = new Color(0.94f, 0.96f, 0.94f, 0.88f);
        batch.setColor(lineCol);

        // Outer Touchlines & Goal Lines
        drawRectBorder(FIELD_X, FIELD_Y, FIELD_W, FIELD_H, 3);

        // Halfway Line (bottom boundary of half pitch)
        batch.draw(whitePixel, FIELD_X, BOTTOM_LINE_Y, FIELD_W, 3);

        // Center Arc (Semi-circle at bottom center extending upward)
        float centerX = FIELD_X + (FIELD_W / 2f);
        float arcRadius = 110f;
        for (int i = 0; i <= 36; i++) {
            float angle = i * (MathUtils.PI / 36f); // 0 to PI
            float ax = centerX + MathUtils.cos(angle) * arcRadius;
            float ay = BOTTOM_LINE_Y + MathUtils.sin(angle) * arcRadius;
            batch.draw(whitePixel, ax - 1.5f, ay - 1.5f, 3f, 3f);
        }
        // Center Spot at bottom line
        batch.draw(whitePixel, centerX - 5f, BOTTOM_LINE_Y - 5f, 10f, 10f);

        // Goal Box (Six-Yard Box at top)
        float goalBoxX = FIELD_X + ((FIELD_W - GOAL_BOX_W) / 2f);
        drawRectBorder(goalBoxX, GOAL_LINE_Y - GOAL_BOX_H, GOAL_BOX_W, GOAL_BOX_H, 2);

        // Penalty Box (at top)
        float penaltyBoxX = FIELD_X + ((FIELD_W - PENALTY_BOX_W) / 2f);
        drawRectBorder(penaltyBoxX, GOAL_LINE_Y - PENALTY_BOX_H, PENALTY_BOX_W, PENALTY_BOX_H, 2);

        // Penalty Spot & Penalty Arc (at top)
        float penaltySpotY = GOAL_LINE_Y - 170f;
        batch.draw(whitePixel, centerX - 4f, penaltySpotY - 4f, 8f, 8f);

        float penaltyArcRadius = 90f;
        float penaltyBoxBottomY = GOAL_LINE_Y - PENALTY_BOX_H;
        for (int i = 0; i <= 36; i++) {
            float angle = MathUtils.PI + i * (MathUtils.PI / 36f); // PI to 2PI (downwards arc)
            float ax = centerX + MathUtils.cos(angle) * penaltyArcRadius;
            float ay = penaltySpotY + MathUtils.sin(angle) * penaltyArcRadius;
            if (ay < penaltyBoxBottomY) {
                batch.draw(whitePixel, ax - 1.5f, ay - 1.5f, 3f, 3f);
            }
        }

        // Corner Arcs (4 Quadrants)
        drawCornerArc(FIELD_X, FIELD_Y, 25f, 0);                 // Bottom-Left
        drawCornerArc(FIELD_X + FIELD_W, FIELD_Y, 25f, 1);      // Bottom-Right
        drawCornerArc(FIELD_X, GOAL_LINE_Y, 25f, 2);             // Top-Left
        drawCornerArc(FIELD_X + FIELD_W, GOAL_LINE_Y, 25f, 3);  // Top-Right

        // 4. Stylized Football Goal Structure (Top of pitch)
        float goalDepth = 40f;

        // Net Mesh Interior
        batch.setColor(1.0f, 1.0f, 1.0f, 0.18f);
        batch.draw(whitePixel, GOAL_X, GOAL_LINE_Y, GOAL_W, goalDepth);
        // Net grid lines
        batch.setColor(1.0f, 1.0f, 1.0f, 0.28f);
        for (float gx = GOAL_X + 20f; gx < GOAL_X + GOAL_W; gx += 20f) {
            batch.draw(whitePixel, gx, GOAL_LINE_Y, 1.5f, goalDepth);
        }
        for (float gy = GOAL_LINE_Y + 10f; gy < GOAL_LINE_Y + goalDepth; gy += 10f) {
            batch.draw(whitePixel, GOAL_X, gy, GOAL_W, 1.5f);
        }

        // Crisp White Goal Posts and Crossbar Frame
        batch.setColor(Color.WHITE);
        batch.draw(whitePixel, GOAL_X - 4f, GOAL_LINE_Y, 5f, goalDepth + 4f);                // Left Post
        batch.draw(whitePixel, GOAL_X + GOAL_W - 1f, GOAL_LINE_Y, 5f, goalDepth + 4f);       // Right Post
        batch.draw(whitePixel, GOAL_X - 4f, GOAL_LINE_Y + goalDepth, GOAL_W + 8f, 5f);       // Back Crossbar
        batch.draw(whitePixel, GOAL_X - 4f, GOAL_LINE_Y, GOAL_W + 8f, 4f);                   // Front Goal Line
    }

    private void drawCornerArc(float cx, float cy, float radius, int quadrant) {
        int steps = 12;
        float startAngle = quadrant * (MathUtils.PI / 2f);
        for (int i = 0; i <= steps; i++) {
            float angle = startAngle + i * (MathUtils.PI / (2f * steps));
            float ax = cx + MathUtils.cos(angle) * radius;
            float ay = cy + MathUtils.sin(angle) * radius;
            batch.draw(whitePixel, ax - 1.5f, ay - 1.5f, 3f, 3f);
        }
    }

    /**
     * Renders human-like top-down player silhouettes with distinct Sky Blue (CSE) and Yellow (EEE) kits.
     * Includes subtle idle bobbing, kick preparation, and reception reaction animation states.
     * NO player drop shadows rendered.
     */
    private void drawHumanPlayers() {
        for (FootballPlayer p : players) {
            Color kitColor = p.getKitColor();

            // Apply procedural offsets (idle bobbing, kick recoil, or reception reaction)
            float bobY = p.getIdleBobbingY(animTime);
            float kickOffsetX = 0f;
            float kickOffsetY = 0f;

            if (p.isKickAnimating()) {
                float kickProg = p.getKickAnimProgress(); // 1.0 -> 0.0
                kickOffsetY = (float) Math.sin(kickProg * MathUtils.PI) * 6f;
            }

            float px = p.getX() + kickOffsetX;
            float py = p.getY() + bobY + kickOffsetY;

            // Dimensions for Top-Down Human Silhouette
            float bodyW = 24f;
            float bodyH = 14f;
            float headR = 6f;
            float shortsW = 16f;
            float shortsH = 7f;

            // Apply slight pulse if reaction animating
            if (p.isReactionAnimating()) {
                bodyW *= 1.15f;
                bodyH *= 1.15f;
            }

            // 1. Boots / Feet (2 small dark blocks)
            batch.setColor(0.12f, 0.12f, 0.15f, 1.0f);
            batch.draw(whitePixel, px - 8f, py - 12f, 5f, 5f);
            batch.draw(whitePixel, px + 3f, py - 12f, 5f, 5f);

            // 2. Shorts (Navy / Dark kit shorts)
            batch.setColor(0.12f, 0.15f, 0.22f, 1.0f);
            batch.draw(whitePixel, px - (shortsW / 2f), py - 7f, shortsW, shortsH);

            // 3. Jersey / Torso & Shoulders (Primary Team Kit Color: Sky Blue for CSE, Yellow for EEE, Orange for GK)
            batch.setColor(kitColor);
            batch.draw(whitePixel, px - (bodyW / 2f), py, bodyW, bodyH);

            // Sleeves / Arms extending left/right
            batch.draw(whitePixel, px - (bodyW / 2f) - 3f, py + 3f, 3f, 8f);
            batch.draw(whitePixel, px + (bodyW / 2f), py + 3f, 3f, 8f);

            // Dark kit border trim
            batch.setColor(0.10f, 0.10f, 0.12f, 0.85f);
            drawRectBorder(px - (bodyW / 2f), py, bodyW, bodyH, 1);

            // 4. Head (Skin / Hair circle atop shoulders)
            batch.setColor(0.20f, 0.15f, 0.10f, 1.0f); // Dark hair / head
            batch.draw(whitePixel, px - headR, py + 8f, headR * 2f, headR * 2f);
            batch.setColor(0.92f, 0.76f, 0.62f, 1.0f); // Face skin tone accent
            batch.draw(whitePixel, px - (headR - 1.5f), py + 9.5f, (headR - 1.5f) * 2f, (headR - 1.5f) * 2f);

            // Special Visual Distinction for MC (Sky Blue kit + Gold Halo Ring + "MC" Label)
            if (p.isMC()) {
                float pulse = (float) Math.sin(animTime * 7f) * 0.30f + 0.70f;
                batch.setColor(FootballPlayer.COLOR_MC_HIGHLIGHT.r, FootballPlayer.COLOR_MC_HIGHLIGHT.g, FootballPlayer.COLOR_MC_HIGHLIGHT.b, pulse);
                drawRectBorder(px - 18f, py - 12f, 36f, 36f, 3);

                // "MC" Text Label above head
                hudFont.setColor(FootballPlayer.COLOR_MC_HIGHLIGHT);
                hudFont.draw(batch, "MC", px - 11f, py + 34f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Renders a solid white circular ball with power-based Z-height arc and subtle ground shadow.
     * Ground pass (<40%): Ball moves flat along grass.
     * Flying pass (>=40%): Ball elevates in parabolic arc while subtle shadow follows ground position.
     */
    private void drawBall() {
        Vector2 groundPos = ball.getGroundPosition();
        Vector2 renderPos = ball.getRenderPosition();
        float zHeight = ball.getZHeight();
        float maxHeight = Math.max(ball.getMaxHeight(), 1.0f);
        float heightRatio = MathUtils.clamp(zHeight / maxHeight, 0f, 1f);

        // 1. Render Subtle Ground Shadow (only when airborne or during flight)
        if (ball.isAirborne() && zHeight > 0f) {
            float shadowW = 16.0f - (heightRatio * 4.0f);
            float shadowH = 8.0f - (heightRatio * 2.0f);
            float shadowAlpha = 0.40f - (heightRatio * 0.18f);

            batch.setColor(0.06f, 0.08f, 0.06f, shadowAlpha);
            batch.draw(whitePixel, groundPos.x - (shadowW / 2f), groundPos.y - (shadowH / 2f), shadowW, shadowH);
        }

        // 2. Render Ball (at Z-elevated position)
        float baseRadius = 8.0f;
        float ballRadius = baseRadius + (heightRatio * 3.5f); // Subtle size increase at peak arc
        float diameter = ballRadius * 2f;

        // Solid White Circle Ball
        batch.setColor(Color.WHITE);
        for (int i = 0; i <= 24; i++) {
            float angle = i * (MathUtils.PI2 / 24f);
            float bx = renderPos.x + MathUtils.cos(angle) * (ballRadius - 1f);
            float by = renderPos.y + MathUtils.sin(angle) * (ballRadius - 1f);
            batch.draw(whitePixel, bx - 1f, by - 1f, 2f, 2f);
        }
        batch.draw(whitePixel, renderPos.x - (ballRadius - 2f), renderPos.y - (ballRadius - 2f), (ballRadius - 2f) * 2f, (ballRadius - 2f) * 2f);

        // Subtle dark outer border ring
        batch.setColor(0.12f, 0.12f, 0.15f, 0.9f);
        drawRectBorder(renderPos.x - ballRadius, renderPos.y - ballRadius, diameter, diameter, 1);
        batch.setColor(Color.WHITE);
    }

    /**
     * Renders ONLY the shooting direction ray originating from MC.
     * NO landing crosshair, NO target marker, NO predicted distance indicator.
     */
    private void drawAimDirectionRay() {
        float mcX = mcPlayer.getX();
        float mcY = mcPlayer.getY();

        // Fixed directional ray length (120 px) showing solely shooting direction
        float rayLength = 120f;
        float dirX = mcX + MathUtils.cos(aimAngle) * rayLength;
        float dirY = mcY + MathUtils.sin(aimAngle) * rayLength;

        // 1. Draw Direction Line Ray
        int dots = 15;
        batch.setColor(new Color(1.0f, 0.90f, 0.30f, 0.90f));
        for (int i = 1; i <= dots; i++) {
            float t = i / (float) dots;
            float dx = MathUtils.lerp(mcX, dirX, t);
            float dy = MathUtils.lerp(mcY, dirY, t);
            batch.draw(whitePixel, dx - 2.5f, dy - 2.5f, 5f, 5f);
        }

        // 2. Direction Arrow Pointer at tip of ray
        batch.setColor(new Color(1.0f, 0.85f, 0.20f, 1.0f));
        batch.draw(whitePixel, dirX - 4f, dirY - 4f, 8f, 8f);

        batch.setColor(Color.WHITE);
    }

    /**
     * Renders screen-space HUD overlay with clean hierarchy and explicit 40% Power Flight Threshold marker.
     */
    private void drawHUD() {
        // 1. Top Screen-Space Header Bar
        batch.setColor(0.08f, 0.10f, 0.14f, 0.92f);
        batch.draw(whitePixel, 0, 930f, VIEW_W, 70f);
        batch.setColor(0.25f, 0.50f, 0.85f, 0.90f);
        batch.draw(whitePixel, 0, 930f, VIEW_W, 3f);

        // Compact Top-Left Scoreboard (Updated via 40s segment evaluations)
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, String.format("CSE %d  -  %d EEE", cseScore, eeeScore), 40f, 974f);

        // Subtle Secondary MC Points Display (PTS: X)
        hudFont.setColor(new Color(0.70f, 0.85f, 1.0f, 0.80f));
        hudFont.draw(batch, String.format("PTS: %d", mcPoints), 280f, 974f);

        // Match Timer & Round Timer (Right)
        hudFont.setColor(roundTimer <= 3.0f ? Color.RED : new Color(1.0f, 0.85f, 0.30f, 1.0f));
        int matchMin = (int) (matchTimer / 60f);
        int matchSec = (int) (matchTimer % 60f);
        hudFont.draw(batch, String.format("MATCH: %d:%02d  |  ROUND: %.1fs", matchMin, matchSec, roundTimer), 1220f, 974f);

        // 2. Power Indicator UI (Anchored on Right Margin) with 40% Airborne Threshold Line
        float barX = 1380f;
        float barY = 160f;
        float barW = 36f;
        float barH = 680f;

        batch.setColor(0.08f, 0.10f, 0.14f, 0.88f);
        batch.draw(whitePixel, barX - 6f, barY - 6f, barW + 12f, barH + 12f);
        batch.setColor(0.25f, 0.50f, 0.85f, 0.85f);
        drawRectBorder(barX - 6f, barY - 6f, barW + 12f, barH + 12f, 2);

        // Fill Bar with Power Tiers (<40% Ground Pass, >=40% Flying Pass)
        float fillH = barH * kickPower;
        Color powerCol;
        String powerTier;
        if (kickPower < 0.40f) {
            powerCol = new Color(0.25f, 0.85f, 0.35f, 0.95f); // Green = Ground Pass
            powerTier = "GROUND PASS";
        } else if (kickPower < 0.70f) {
            powerCol = new Color(1.0f, 0.80f, 0.20f, 0.95f); // Gold = Low-Med Flight
            powerTier = "FLYING (MED)";
        } else {
            powerCol = new Color(1.0f, 0.30f, 0.25f, 0.95f); // Red = High Flight
            powerTier = "FLYING (HIGH)";
        }

        batch.setColor(powerCol);
        batch.draw(whitePixel, barX, barY, barW, fillH);

        // Tier Indicator Notches (70% High Flight Notch)
        batch.setColor(1.0f, 1.0f, 1.0f, 0.50f);
        batch.draw(whitePixel, barX, barY + barH * 0.70f, barW, 2f);

        // Labels
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, String.format("%.0f%%", kickPower * 100f), barX - 6f, barY - 14f);

        hudFont.setColor(powerCol);
        hudFont.draw(batch, powerTier, barX - 16f, barY + fillH + 20f);

        hudFont.setColor(new Color(0.80f, 0.90f, 1.0f, 0.9f));
        hudFont.draw(batch, "POWER", barX - 12f, barY + barH + 34f);

        // 3. Bottom Controls Banner
        batch.setColor(0.08f, 0.10f, 0.14f, 0.92f);
        batch.draw(whitePixel, 0, 0, VIEW_W, 50f);
        batch.setColor(0.25f, 0.50f, 0.85f, 0.90f);
        batch.draw(whitePixel, 0, 48f, VIEW_W, 2f);

        hudFont.setColor(new Color(0.85f, 0.92f, 1.0f, 0.95f));
        hudFont.draw(batch, "CONTROLS:  [A / D] AIM DIRECTION  |  [W / S] POWER  |  [SPACE] KICK PASS  |  [ESC] EXIT MATCH", 250f, 32f);

        batch.setColor(Color.WHITE);
    }

    /**
     * Renders small sideline commentary speech bubbles stacked vertically near the left touchline margin.
     */
    private void drawSidelineCommentary() {
        if (previousGroup != null && crossfadeTimer > 0f) {
            float prevAlpha = crossfadeTimer / CROSSFADE_DURATION;
            renderCommentaryGroup(previousGroup, prevAlpha);
        }
        if (activeGroup != null) {
            float activeAlpha = crossfadeTimer > 0f ? (1.0f - (crossfadeTimer / CROSSFADE_DURATION)) : 1.0f;
            renderCommentaryGroup(activeGroup, activeAlpha);
        }
    }

    private void renderCommentaryGroup(CommentaryGroup group, float groupAlpha) {
        if (group == null || group.items == null || group.items.isEmpty() || groupAlpha <= 0f) return;

        hudFont.getData().setScale(1.15f);

        float startX = 30.0f; // Positioned on left touchline margin
        float startY = FIELD_Y + (FIELD_H / 2.0f) + 80.0f;
        float lineSpacing = 48.0f;

        for (int i = 0; i < group.items.size(); i++) {
            CommentaryItem item = group.items.get(i);
            layout.setText(hudFont, item.text);

            float bubbleW = layout.width + 32.0f;
            float bubbleH = 40.0f;
            float bubbleX = startX;
            float bubbleY = startY - (i * lineSpacing);

            // Translucent Dark Shout Bubble Backing
            batch.setColor(0.06f, 0.08f, 0.12f, 0.90f * groupAlpha);
            batch.draw(whitePixel, bubbleX, bubbleY, bubbleW, bubbleH);

            Color borderCol = new Color(item.color.r, item.color.g, item.color.b, item.color.a * groupAlpha);
            batch.setColor(borderCol);
            drawRectBorder(bubbleX, bubbleY, bubbleW, bubbleH, 2);

            // Speech bubble pointer facing right toward pitch
            batch.draw(whitePixel, bubbleX + bubbleW, bubbleY + 14.0f, 7.0f, 10.0f);

            hudFont.setColor(borderCol);
            hudFont.draw(batch, item.text, bubbleX + 16.0f, bubbleY + 28.0f);
        }

        hudFont.getData().setScale(1.25f);
        batch.setColor(Color.WHITE);
    }

    private void drawMatchOverModal() {
        // Dark backdrop
        batch.setColor(0f, 0f, 0f, 0.84f);
        batch.draw(whitePixel, 0, 0, VIEW_W, VIEW_H);

        float modalW = 560f;
        float modalH = 260f;
        float modalX = (VIEW_W - modalW) / 2f;
        float modalY = (VIEW_H - modalH) / 2f;

        batch.setColor(0.10f, 0.12f, 0.18f, 0.96f);
        batch.draw(whitePixel, modalX, modalY, modalW, modalH);

        batch.setColor(0.35f, 0.75f, 1.0f, 0.95f);
        drawRectBorder(modalX, modalY, modalW, modalH, 3);

        titleFont.setColor(new Color(1.0f, 0.85f, 0.25f, 1.0f));
        String scoreText = String.format("CSE %d  -  %d EEE", cseScore, eeeScore);
        layout.setText(titleFont, scoreText);
        titleFont.draw(batch, scoreText, modalX + (modalW - layout.width) / 2f, modalY + modalH - 75f);

        float pulse = (float) Math.sin(animTime * 6f) * 0.25f + 0.75f;
        hudFont.setColor(0.35f, 0.85f, 1.0f, pulse);
        layout.setText(hudFont, "PRESS [ENTER] TO RETURN");
        hudFont.draw(batch, "PRESS [ENTER] TO RETURN", modalX + (modalW - layout.width) / 2f, modalY + 60f);

        batch.setColor(Color.WHITE);
    }

    private void drawRectBorder(float x, float y, float w, float h, int thickness) {
        batch.draw(whitePixel, x, y, w, thickness);
        batch.draw(whitePixel, x, y + h - thickness, w, thickness);
        batch.draw(whitePixel, x, y, thickness, h);
        batch.draw(whitePixel, x + w - thickness, y, thickness, h);
    }

    private void returnToGameScreen() {
        if (resultListener != null && state == GameState.MATCH_OVER) {
            resultListener.onMatchCompleted(cseScore, eeeScore);
        }
        if (previousScreen != null) {
            game.setScreen(previousScreen);
        }
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (whitePixel != null) whitePixel.dispose();
        if (titleFont != null) titleFont.dispose();
        if (hudFont != null) hudFont.dispose();
        if (toastFont != null) toastFont.dispose();
    }
}
