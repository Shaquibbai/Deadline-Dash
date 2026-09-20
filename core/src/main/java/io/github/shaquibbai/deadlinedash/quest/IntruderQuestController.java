package io.github.shaquibbai.deadlinedash.quest;

import io.github.shaquibbai.deadlinedash.rep.RepSystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Quest: Intruder Detection.
 * Manages quest states (NOT_STARTED, INVESTIGATION, COMPLETED),
 * Shihab quest giver interactions, the 10 event NPCs and their registration numbers,
 * dynamic overhead markers (Yellow ?, Green ✓, Red ✕), and remaining intruder calculations.
 */
public class IntruderQuestController {
    public static final String QUEST_GIVER = "Shihab";

    public static final String DIALOGUE_GIVER_INTRO = "intruder_quest_intro";
    public static final String DIALOGUE_GIVER_WAITING = "intruder_giver_waiting";
    public static final String DIALOGUE_GIVER_COMPLETE = "intruder_giver_complete";
    public static final String DIALOGUE_ALREADY_CHECKED = "intruder_npc_already_checked";

    public enum State {
        NOT_STARTED,
        INVESTIGATION,
        COMPLETED
    }

    public static class QuestNPC {
        private final String name;
        private final int registrationNumber;
        private final String dialogueId;
        private boolean investigated = false;

        public QuestNPC(String name, int registrationNumber, String dialogueId) {
            this.name = name;
            this.registrationNumber = registrationNumber;
            this.dialogueId = dialogueId;
        }

        public String getName() {
            return name;
        }

        public int getRegistrationNumber() {
            return registrationNumber;
        }

        public String getDialogueId() {
            return dialogueId;
        }

        public boolean isInvestigated() {
            return investigated;
        }

        public void setInvestigated(boolean investigated) {
            this.investigated = investigated;
        }

        public boolean isIntruder() {
            return registrationNumber % 2 != 0;
        }
    }

    private State state = State.NOT_STARTED;
    private boolean startPromptActive = false;
    private boolean intruderAlertActive = false;
    private boolean completionBannerActive = false;

    private String lastAlertNpcName = "";
    private int lastAlertRegNumber = 0;
    private int lastAlertFoundCount = 0;

    private final Map<String, QuestNPC> npcs = new LinkedHashMap<>();

    public IntruderQuestController() {
        initNpcs();
    }

    private void initNpcs() {
        npcs.clear();
        // 5 Valid NPCs (Even registration numbers)
        addNpc(new QuestNPC("Tanvir", 124, "intruder_npc_124"));
        addNpc(new QuestNPC("Farhan", 208, "intruder_npc_208"));
        addNpc(new QuestNPC("Aayan", 316, "intruder_npc_316"));
        addNpc(new QuestNPC("Nafis", 422, "intruder_npc_422"));
        addNpc(new QuestNPC("Sami", 276, "intruder_npc_276"));

        // 5 Intruder NPCs (Odd registration numbers)
        addNpc(new QuestNPC("Shadman", 137, "intruder_npc_137"));
        addNpc(new QuestNPC("Kazi", 151, "intruder_npc_151"));
        addNpc(new QuestNPC("Adnan", 167, "intruder_npc_167"));
        addNpc(new QuestNPC("Fahim", 189, "intruder_npc_189"));
        addNpc(new QuestNPC("Tariq", 205, "intruder_npc_205"));
    }

    private void addNpc(QuestNPC npc) {
        npcs.put(npc.getName().toLowerCase(), npc);
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

    public boolean isStartPromptActive() {
        return startPromptActive;
    }

    public void setStartPromptActive(boolean active) {
        this.startPromptActive = active;
    }

    public boolean isIntruderAlertActive() {
        return intruderAlertActive;
    }

    public void dismissIntruderAlert() {
        this.intruderAlertActive = false;
    }

    public boolean isCompletionBannerActive() {
        return completionBannerActive;
    }

    public void dismissCompletionBanner() {
        this.completionBannerActive = false;
    }

    public String getLastAlertNpcName() {
        return lastAlertNpcName;
    }

    public int getLastAlertRegNumber() {
        return lastAlertRegNumber;
    }

    public int getLastAlertFoundCount() {
        return lastAlertFoundCount;
    }

    public Map<String, QuestNPC> getNpcs() {
        return Collections.unmodifiableMap(npcs);
    }

    public QuestNPC getNpc(String name) {
        if (name == null) return null;
        return npcs.get(name.trim().toLowerCase());
    }

    public boolean isManagedNpc(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (QUEST_GIVER.equalsIgnoreCase(trimmed)) return true;
        return npcs.containsKey(trimmed.toLowerCase());
    }

    public boolean isNpcInteractable(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (QUEST_GIVER.equalsIgnoreCase(trimmed)) {
            return true;
        }

        QuestNPC npc = npcs.get(trimmed.toLowerCase());
        if (npc != null) {
            return state == State.INVESTIGATION || state == State.COMPLETED;
        }

        return true;
    }

    public QuestMarker getMarkerForNpc(String name) {
        if (name == null) return QuestMarker.NONE;
        String trimmed = name.trim();

        if (QUEST_GIVER.equalsIgnoreCase(trimmed)) {
            if (state == State.NOT_STARTED) {
                return QuestMarker.QUEST_AVAILABLE; // Yellow ?
            }
            return QuestMarker.NONE;
        }

        QuestNPC npc = npcs.get(trimmed.toLowerCase());
        if (npc != null) {
            if (state == State.INVESTIGATION) {
                if (!npc.isInvestigated()) {
                    return QuestMarker.QUEST_AVAILABLE; // Yellow ?
                } else if (npc.isIntruder()) {
                    return QuestMarker.INTRUDER; // Red ✕
                } else {
                    return QuestMarker.VALID; // Green ✓
                }
            } else if (state == State.COMPLETED) {
                if (npc.isInvestigated()) {
                    return npc.isIntruder() ? QuestMarker.INTRUDER : QuestMarker.VALID;
                }
            }
        }

        return QuestMarker.NONE;
    }

    public String getDialogueForNpc(String name, String defaultDialogueId) {
        if (name == null) return defaultDialogueId;
        String trimmed = name.trim();

        if (QUEST_GIVER.equalsIgnoreCase(trimmed)) {
            switch (state) {
                case NOT_STARTED:
                    return DIALOGUE_GIVER_INTRO;
                case INVESTIGATION:
                    return DIALOGUE_GIVER_WAITING;
                case COMPLETED:
                default:
                    return DIALOGUE_GIVER_COMPLETE;
            }
        }

        QuestNPC npc = npcs.get(trimmed.toLowerCase());
        if (npc != null) {
            if (state == State.INVESTIGATION) {
                if (!npc.isInvestigated()) {
                    return npc.getDialogueId();
                } else {
                    return DIALOGUE_ALREADY_CHECKED;
                }
            } else if (state == State.COMPLETED) {
                return DIALOGUE_ALREADY_CHECKED;
            }
        }

        return defaultDialogueId;
    }

    public int getTotalIntruders() {
        return 5;
    }

    public int getDetectedIntrudersCount() {
        int count = 0;
        for (QuestNPC npc : npcs.values()) {
            if (npc.isInvestigated() && npc.isIntruder()) {
                count++;
            }
        }
        return count;
    }

    public int getRemainingIntrudersCount() {
        return Math.max(0, getTotalIntruders() - getDetectedIntrudersCount());
    }

    /**
     * Called when a dialogue finishes.
     * Evaluates quest giver start prompt, or investigates NPC and updates counts.
     */
    public boolean onDialogueCompleted(String dialogueId, String npcName, RepSystem repSystem) {
        if (dialogueId == null || npcName == null) return false;
        String trimmedDialogue = dialogueId.trim();
        String trimmedNpc = npcName.trim();

        // Quest Giver dialogue completed
        if (QUEST_GIVER.equalsIgnoreCase(trimmedNpc) && DIALOGUE_GIVER_INTRO.equalsIgnoreCase(trimmedDialogue)) {
            if (state == State.NOT_STARTED) {
                startPromptActive = true;
                return true;
            }
            return false;
        }

        // Investigation NPC dialogue completed
        QuestNPC npc = npcs.get(trimmedNpc.toLowerCase());
        if (npc != null && state == State.INVESTIGATION) {
            if (!npc.isInvestigated()) {
                npc.setInvestigated(true);

                if (npc.isIntruder()) {
                    lastAlertNpcName = npc.getName();
                    lastAlertRegNumber = npc.getRegistrationNumber();
                    lastAlertFoundCount = getDetectedIntrudersCount();
                    intruderAlertActive = true;

                    System.out.printf("[INTRUDER QUEST] Intruder detected: %s (Reg: %d). Found %d/5.%n",
                        npc.getName(), npc.getRegistrationNumber(), lastAlertFoundCount);

                    if (getRemainingIntrudersCount() == 0) {
                        state = State.COMPLETED;
                        completionBannerActive = true;
                        if (repSystem != null) {
                            repSystem.addRep(35); // Award REP for completing intruder hunt
                        }
                        System.out.println("[INTRUDER QUEST] All 5 intruders found! Quest completed.");
                    }
                    return true;
                } else {
                    System.out.printf("[INTRUDER QUEST] Valid NPC verified: %s (Reg: %d).%n",
                        npc.getName(), npc.getRegistrationNumber());
                }
            }
        }

        return false;
    }

    public void onStartChoiceYes() {
        this.startPromptActive = false;
        this.state = State.INVESTIGATION;
        System.out.println("[INTRUDER QUEST] Intruder hunt started! Investigation phase active.");
    }

    public void onStartChoiceNo() {
        this.startPromptActive = false;
        this.state = State.NOT_STARTED;
        System.out.println("[INTRUDER QUEST] Intruder hunt postponed.");
    }
}
