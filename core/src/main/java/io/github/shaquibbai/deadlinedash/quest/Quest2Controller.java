package io.github.shaquibbai.deadlinedash.quest;

import io.github.shaquibbai.deadlinedash.inventory.Backpack;
import io.github.shaquibbai.deadlinedash.rep.RepSystem;
import io.github.shaquibbai.deadlinedash.timer.TimeManager;

/**
 * Controller for Quest 2 (Football Quest).
 * Single authoritative state machine managing Shafeen & Talha interactability, markers,
 * dialogues, Yes/No football match prompt, minigame result handling, time/REP adjustments, and persistence.
 */
public class Quest2Controller {
    public static final String SHAFEEN = "Shafeen";
    public static final String TALHA = "Talha";

    public static final String DIALOGUE_SHAFEEN_INTRO = "Shafeen_intro";
    public static final String DIALOGUE_SHAFEEN_WAITING = "Shafeen_waiting";
    public static final String DIALOGUE_TALHA_INTRO = "Talha_intro";
    public static final String DIALOGUE_TALHA_WAITING = "Talha_waiting";
    public static final String DIALOGUE_TALHA_MATCH_WON = "Talha_matchWon";
    public static final String DIALOGUE_TALHA_MATCH_LOST = "Talha_matchLost";

    public enum State {
        SHAFEEN_INTRO,
        TALHA_INTRO,
        TALHA_WAITING,
        COMPLETED
    }

    public interface Quest2CompletionListener {
        void onQuest2Completed();
    }

    private State state = State.SHAFEEN_INTRO;
    private boolean matchPromptActive = false;
    private boolean rewardsApplied = false;
    private Quest2CompletionListener completionListener;

    public void setCompletionListener(Quest2CompletionListener listener) {
        this.completionListener = listener;
    }

    private Integer lastCseScore = null;
    private Integer lastEeeScore = null;

    public Quest2Controller() {
    }

    public Quest2Controller(String questJson) {
        this();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isQuestCompleted() {
        return state == State.COMPLETED;
    }

    public boolean isMatchPromptActive() {
        return matchPromptActive;
    }

    public void setMatchPromptActive(boolean active) {
        this.matchPromptActive = active;
    }

    public boolean areRewardsApplied() {
        return rewardsApplied;
    }

    public boolean isManagedNpc(String npcName) {
        if (npcName == null) return false;
        String trimmed = npcName.trim();
        return SHAFEEN.equalsIgnoreCase(trimmed) || TALHA.equalsIgnoreCase(trimmed);
    }

    public boolean isNpcInteractable(String npcName) {
        if (npcName == null) return false;
        String trimmed = npcName.trim();
        switch (state) {
            case SHAFEEN_INTRO:
                return SHAFEEN.equalsIgnoreCase(trimmed);
            case TALHA_INTRO:
            case TALHA_WAITING:
                return SHAFEEN.equalsIgnoreCase(trimmed) || TALHA.equalsIgnoreCase(trimmed);
            case COMPLETED:
            default:
                return false;
        }
    }

    public QuestMarker getMarkerForNpc(String npcName, Backpack backpack) {
        if (npcName == null) return QuestMarker.NONE;
        String trimmed = npcName.trim();
        switch (state) {
            case SHAFEEN_INTRO:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return QuestMarker.NEW_INTERACTION; // Yellow !
                return QuestMarker.NONE;
            case TALHA_INTRO:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return QuestMarker.REMINDER; // Red ?
                if (TALHA.equalsIgnoreCase(trimmed)) return QuestMarker.NEW_INTERACTION; // Yellow !
                return QuestMarker.NONE;
            case TALHA_WAITING:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return QuestMarker.REMINDER; // Red ?
                if (TALHA.equalsIgnoreCase(trimmed)) return QuestMarker.REMINDER; // Red ?
                return QuestMarker.NONE;
            case COMPLETED:
            default:
                return QuestMarker.NONE;
        }
    }

    public String getDialogueForNpc(String npcName, String defaultDialogueId, Backpack backpack) {
        if (npcName == null) return defaultDialogueId;
        String trimmed = npcName.trim();
        switch (state) {
            case SHAFEEN_INTRO:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return DIALOGUE_SHAFEEN_INTRO;
                return defaultDialogueId;
            case TALHA_INTRO:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return DIALOGUE_SHAFEEN_WAITING;
                if (TALHA.equalsIgnoreCase(trimmed)) return DIALOGUE_TALHA_INTRO;
                return defaultDialogueId;
            case TALHA_WAITING:
                if (SHAFEEN.equalsIgnoreCase(trimmed)) return DIALOGUE_SHAFEEN_WAITING;
                if (TALHA.equalsIgnoreCase(trimmed)) return DIALOGUE_TALHA_WAITING;
                return defaultDialogueId;
            case COMPLETED:
            default:
                return defaultDialogueId;
        }
    }

    public boolean onDialogueCompleted(String dialogueId, String npcName, Backpack backpack, RepSystem repSystem, TimeManager timeManager) {
        if (dialogueId == null || npcName == null || isQuestCompleted()) {
            return false;
        }

        String trimmedDialogue = dialogueId.trim();
        String trimmedNpc = npcName.trim();

        if (SHAFEEN.equalsIgnoreCase(trimmedNpc) && DIALOGUE_SHAFEEN_INTRO.equalsIgnoreCase(trimmedDialogue)) {
            if (state == State.SHAFEEN_INTRO) {
                state = State.TALHA_INTRO;
            }
            return false;
        }

        if (TALHA.equalsIgnoreCase(trimmedNpc)) {
            if (DIALOGUE_TALHA_INTRO.equalsIgnoreCase(trimmedDialogue)) {
                state = State.TALHA_WAITING;
                matchPromptActive = true;
                return true;
            }

            if (DIALOGUE_TALHA_WAITING.equalsIgnoreCase(trimmedDialogue)) {
                state = State.TALHA_WAITING;
                matchPromptActive = true;
                return true;
            }

            if (DIALOGUE_TALHA_MATCH_WON.equalsIgnoreCase(trimmedDialogue)) {
                completeQuestWithResult(true, repSystem, timeManager);
                return false;
            }

            if (DIALOGUE_TALHA_MATCH_LOST.equalsIgnoreCase(trimmedDialogue)) {
                completeQuestWithResult(false, repSystem, timeManager);
                return false;
            }
        }

        return false;
    }

    public void completeQuestWithResult(boolean cseWon, RepSystem repSystem, TimeManager timeManager) {
        if (rewardsApplied) {
            state = State.COMPLETED;
            matchPromptActive = false;
            return;
        }

        int cseScore = lastCseScore != null ? lastCseScore : (cseWon ? 1 : 0);
        int eeeScore = lastEeeScore != null ? lastEeeScore : (cseWon ? 0 : 1);
        int goalDiff = Math.max(1, Math.abs(cseScore - eeeScore));

        if (cseWon) {
            if (timeManager != null) {
                timeManager.setTime(timeManager.getTime() - (5f * TimeManager.SECONDS_PER_GAME_HOUR));
            }
            int repGain;
            switch (goalDiff) {
                case 1: repGain = 20; break;
                case 2: repGain = 30; break;
                case 3: repGain = 35; break;
                default: repGain = 40; break;
            }
            if (repSystem != null) {
                repSystem.addRep(repGain);
            }
        } else {
            if (timeManager != null) {
                timeManager.setTime(timeManager.getTime() - (10f * TimeManager.SECONDS_PER_GAME_HOUR));
            }
            int repLoss;
            switch (goalDiff) {
                case 1: repLoss = 10; break;
                case 2: repLoss = 20; break;
                case 3: repLoss = 25; break;
                default: repLoss = 30; break;
            }
            if (repSystem != null) {
                repSystem.removeRep(repLoss);
            }
        }

        rewardsApplied = true;
        state = State.COMPLETED;
        matchPromptActive = false;
        if (completionListener != null) {
            completionListener.onQuest2Completed();
        }
    }

    public String handleMatchResult(int cseScore, int eeeScore) {
        this.lastCseScore = cseScore;
        this.lastEeeScore = eeeScore;
        this.state = State.TALHA_WAITING;
        this.matchPromptActive = false;

        boolean cseWon = cseScore > eeeScore;
        return cseWon ? DIALOGUE_TALHA_MATCH_WON : DIALOGUE_TALHA_MATCH_LOST;
    }

    public void onMatchChoiceYes() {
        this.matchPromptActive = false;
    }

    public void onMatchChoiceNo() {
        this.matchPromptActive = false;
        this.state = State.TALHA_WAITING;
    }
}
