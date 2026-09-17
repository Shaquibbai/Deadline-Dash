package io.github.shaquibbai.deadlinedash.quest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.github.shaquibbai.deadlinedash.inventory.Backpack;
import io.github.shaquibbai.deadlinedash.inventory.Item;
import io.github.shaquibbai.deadlinedash.npc.NPC;
import io.github.shaquibbai.deadlinedash.rep.RepSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic data-driven state machine manager for Quests.
 * Manages quest step progression, dynamic NPC interactability, NPC quest markers (Yellow ! / Red ?),
 * quest dialogue selection overrides, single/multiple inventory item handoffs, and one-time REP rewards.
 */
public class QuestManager {
    private Quest quest;
    private QuestState questState = QuestState.NOT_STARTED;
    private int currentStepIndex = 1;
    private final Set<String> interactableNpcs = new HashSet<>();
    private final Set<String> managedNpcs = new HashSet<>();

    private int completedTaskCount = 0;
    private final Set<String> completedQuestIds = new HashSet<>();

    public interface QuestCompletionListener {
        void onQuestCompleted(String questId, int totalCompletedTasks);
    }
    private QuestCompletionListener questCompletionListener;

    public void setQuestCompletionListener(QuestCompletionListener listener) {
        this.questCompletionListener = listener;
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public boolean recordTaskCompleted(String questId) {
        if (questId != null && !questId.trim().isEmpty() && completedQuestIds.add(questId.trim())) {
            completedTaskCount++;
            System.out.printf("[QUEST] Main task '%s' recorded as completed. Total completed tasks: %d%n",
                questId.trim(), completedTaskCount);
            if (questCompletionListener != null) {
                questCompletionListener.onQuestCompleted(questId.trim(), completedTaskCount);
            }
            return true;
        }
        return false;
    }

    public QuestManager() {
    }

    public QuestManager(String questJsonPath) {
        loadQuestFromPath(questJsonPath);
    }

    /**
     * Attempts to load a quest definition from LibGDX internal assets path.
     */
    public boolean loadQuestFromPath(String path) {
        if (Gdx.files == null) {
            System.err.println("[QUEST] Gdx.files not available to load path: " + path);
            return false;
        }
        try {
            FileHandle handle = Gdx.files.internal(path);
            if (!handle.exists()) {
                System.err.println("[QUEST] Quest file not found: " + path);
                return false;
            }
            return loadQuestFromString(handle.readString("UTF-8"));
        } catch (Exception e) {
            System.err.println("[QUEST] Failed to load quest from path " + path + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Parses JSON string into a Quest object and initializes quest state.
     */
    public boolean loadQuestFromString(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return false;
        }

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(jsonContent);
            if (root == null) return false;

            String id = root.getString("id", "Quest1");
            String name = root.getString("name", "");
            String description = root.getString("description", "");
            int rewardRep = root.getInt("rewardRep", 0);

            List<String> initialInteractables = new ArrayList<>();
            JsonValue initArray = root.get("initialInteractableNpcs");
            if (initArray != null && initArray.isArray()) {
                for (JsonValue val = initArray.child; val != null; val = val.next) {
                    initialInteractables.add(val.asString());
                }
            }

            List<QuestStep> steps = new ArrayList<>();
            JsonValue stepsArray = root.get("steps");
            if (stepsArray != null && stepsArray.isArray()) {
                for (JsonValue stepVal = stepsArray.child; stepVal != null; stepVal = stepVal.next) {
                    int sIndex = stepVal.getInt("stepIndex", steps.size() + 1);
                    String npc = stepVal.getString("npc", "");
                    String dialogueId = stepVal.getString("dialogueId", "");
                    String reminderDialogueId = stepVal.getString("reminderDialogueId", null);

                    List<String> reminderNpcs = new ArrayList<>();
                    JsonValue remArray = stepVal.get("reminderNpcs");
                    if (remArray != null && remArray.isArray()) {
                        for (JsonValue rVal = remArray.child; rVal != null; rVal = rVal.next) {
                            reminderNpcs.add(rVal.asString());
                        }
                    }

                    List<String> reqItems = new ArrayList<>();
                    JsonValue reqArray = stepVal.get("requiredItems");
                    if (reqArray != null && reqArray.isArray()) {
                        for (JsonValue itemVal = reqArray.child; itemVal != null; itemVal = itemVal.next) {
                            reqItems.add(itemVal.asString());
                        }
                    } else if (stepVal.has("requiredItem")) {
                        String singleReq = stepVal.getString("requiredItem", null);
                        if (singleReq != null && !singleReq.trim().isEmpty()) {
                            reqItems.add(singleReq);
                        }
                    }

                    List<String> removeItems = new ArrayList<>();
                    JsonValue removeArray = stepVal.get("removeItems");
                    if (removeArray != null && removeArray.isArray()) {
                        for (JsonValue itemVal = removeArray.child; itemVal != null; itemVal = itemVal.next) {
                            removeItems.add(itemVal.asString());
                        }
                    } else if (stepVal.has("removeItem")) {
                        String singleRem = stepVal.getString("removeItem", null);
                        if (singleRem != null && !singleRem.trim().isEmpty()) {
                            removeItems.add(singleRem);
                        }
                    }

                    String addItem = stepVal.getString("addItem", null);
                    String addItemMessage = stepVal.getString("addItemMessage", null);

                    List<String> unlockNpcs = new ArrayList<>();
                    JsonValue unlockArray = stepVal.get("unlockNpcs");
                    if (unlockArray != null && unlockArray.isArray()) {
                        for (JsonValue uVal = unlockArray.child; uVal != null; uVal = uVal.next) {
                            unlockNpcs.add(uVal.asString());
                        }
                    }

                    List<String> lockNpcs = new ArrayList<>();
                    JsonValue lockArray = stepVal.get("lockNpcs");
                    if (lockArray != null && lockArray.isArray()) {
                        for (JsonValue lVal = lockArray.child; lVal != null; lVal = lVal.next) {
                            lockNpcs.add(lVal.asString());
                        }
                    }

                    steps.add(new QuestStep(sIndex, npc, dialogueId, reminderDialogueId, reminderNpcs,
                        reqItems, removeItems, addItem, addItemMessage, unlockNpcs, lockNpcs));
                }
            }

            this.quest = new Quest(id, name, description, rewardRep, initialInteractables, steps);
            resetState();
            return true;
        } catch (Exception e) {
            System.err.println("[QUEST] Error parsing quest JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resets state to start of loaded quest.
     */
    public void resetTaskCompletions() {
        this.completedTaskCount = 0;
        this.completedQuestIds.clear();
    }

    public void resetState() {
        resetTaskCompletions();
        if (quest == null) return;
        this.questState = QuestState.IN_PROGRESS;
        this.currentStepIndex = 1;
        this.interactableNpcs.clear();
        this.managedNpcs.clear();

        for (String npcName : quest.getInitialInteractableNpcs()) {
            interactableNpcs.add(npcName.trim());
        }

        for (QuestStep step : quest.getSteps()) {
            if (!step.getNpc().isEmpty()) {
                managedNpcs.add(step.getNpc());
            }
            for (String unlock : step.getUnlockNpcs()) {
                managedNpcs.add(unlock.trim());
            }
        }
    }

    public Quest getQuest() {
        return quest;
    }

    public QuestState getQuestState() {
        return questState;
    }

    public boolean isQuestCompleted() {
        return questState == QuestState.COMPLETED;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public boolean isNpcInteractable(String npcName) {
        if (npcName == null) return false;
        String trimmed = npcName.trim();
        if (questState == QuestState.COMPLETED) {
            return false;
        }
        if (managedNpcs.contains(trimmed)) {
            return interactableNpcs.contains(trimmed);
        }
        return true;
    }

    /**
     * Synchronizes interactable flag on given NPC instance.
     */
    public void syncNpcInteractability(NPC npc) {
        if (npc != null && managedNpcs.contains(npc.getName())) {
            npc.setInteractable(isNpcInteractable(npc.getName()));
        }
    }

    /**
     * Synchronizes interactable flags on all NPCs in given list/collection.
     */
    public void syncMapNpcs(Iterable<NPC> npcs) {
        if (npcs == null) return;
        for (NPC npc : npcs) {
            syncNpcInteractability(npc);
        }
    }

    private boolean hasAllRequiredItems(QuestStep step, Backpack backpack) {
        if (!step.hasRequiredItems()) return true;
        if (backpack == null) return false;
        for (String reqItemName : step.getRequiredItems()) {
            if (!backpack.hasItem(new Item(reqItemName))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines the current QuestMarker (Yellow ! or Red ?) for an NPC.
     */
    public QuestMarker getMarkerForNpc(NPC npc, Backpack backpack) {
        if (npc == null) return QuestMarker.NONE;
        return getMarkerForNpc(npc.getName(), backpack);
    }

    /**
     * Determines the current QuestMarker (Yellow ! or Red ?) for an NPC by name.
     */
    public QuestMarker getMarkerForNpc(String npcName, Backpack backpack) {
        if (quest == null || questState != QuestState.IN_PROGRESS || npcName == null) {
            return QuestMarker.NONE;
        }

        String trimmedNpc = npcName.trim();
        if (!isNpcInteractable(trimmedNpc)) {
            return QuestMarker.NONE;
        }

        QuestStep currentStep = quest.getStep(currentStepIndex);
        if (currentStep == null) {
            return QuestMarker.NONE;
        }

        // Active target NPC for this step
        if (currentStep.getNpc().equalsIgnoreCase(trimmedNpc)) {
            if (hasAllRequiredItems(currentStep, backpack)) {
                return QuestMarker.NEW_INTERACTION; // Yellow !
            } else {
                return QuestMarker.REMINDER; // Red ?
            }
        }

        // Check if NPC is in reminder list for active step
        for (String remNpc : currentStep.getReminderNpcs()) {
            if (remNpc.equalsIgnoreCase(trimmedNpc)) {
                return QuestMarker.REMINDER; // Red ?
            }
        }

        return QuestMarker.NONE;
    }

    /**
     * Determines the active dialogue ID to trigger for an interactable NPC based on quest state.
     */
    public String getDialogueForNpc(NPC npc, Backpack backpack) {
        if (npc == null) return "";
        return getDialogueForNpc(npc.getName(), npc.getConfig().getDialogue(), backpack);
    }

    /**
     * Determines the active dialogue ID to trigger for an NPC name.
     */
    public String getDialogueForNpc(String npcName, String defaultDialogueId, Backpack backpack) {
        if (quest == null || questState != QuestState.IN_PROGRESS || npcName == null) {
            return defaultDialogueId;
        }

        String trimmedNpc = npcName.trim();
        QuestStep currentStep = quest.getStep(currentStepIndex);
        if (currentStep == null) {
            return defaultDialogueId;
        }

        if (currentStep.getNpc().equalsIgnoreCase(trimmedNpc)) {
            if (hasAllRequiredItems(currentStep, backpack)) {
                return currentStep.getDialogueId();
            } else {
                return currentStep.hasReminderDialogue() ? currentStep.getReminderDialogueId() : defaultDialogueId;
            }
        }

        for (String remNpc : currentStep.getReminderNpcs()) {
            if (remNpc.equalsIgnoreCase(trimmedNpc) && currentStep.hasReminderDialogue()) {
                return currentStep.getReminderDialogueId();
            }
        }

        return defaultDialogueId;
    }

    /**
     * Evaluates dialogue completion and transitions quest step state when dialogue finishes.
     * Only designated NEW_INTERACTION dialogues advance quest state; reminder dialogues are informational only.
     * Returns notification toast message string if an item was awarded, or null otherwise.
     */
    public String onDialogueCompleted(String dialogueId, String npcName, Backpack backpack, RepSystem repSystem) {
        if (quest == null || questState != QuestState.IN_PROGRESS || dialogueId == null || npcName == null) {
            return null;
        }

        String trimmedDialogue = dialogueId.trim();
        String trimmedNpc = npcName.trim();

        QuestStep step = quest.getStep(currentStepIndex);
        if (step == null) return null;

        // Verify completed dialogue is the active step's designated NEW_INTERACTION dialogue
        if (step.getDialogueId().equalsIgnoreCase(trimmedDialogue) && step.getNpc().equalsIgnoreCase(trimmedNpc)) {
            if (!hasAllRequiredItems(step, backpack)) {
                return null;
            }

            // Step completed!
            // 1. Remove consumed items if applicable
            if (step.hasRemoveItems() && backpack != null) {
                for (String remItemName : step.getRemoveItems()) {
                    backpack.removeItem(new Item(remItemName));
                    System.out.printf("[QUEST] Removed consumed item '%s' from backpack upon completing dialogue '%s'%n",
                        remItemName, trimmedDialogue);
                }
            }

            // 2. Add rewarded item if applicable
            String feedbackMsg = null;
            if (step.hasAddItem() && backpack != null) {
                backpack.addItem(new Item(step.getAddItem()));
                feedbackMsg = step.getAddItemMessage() != null ? step.getAddItemMessage() : (step.getAddItem() + " added to backpack");
                System.out.printf("[QUEST] Added item '%s' to backpack upon completing dialogue '%s'%n",
                    step.getAddItem(), trimmedDialogue);
            }

            // 3. Apply NPC unlocks and locks
            for (String unlock : step.getUnlockNpcs()) {
                interactableNpcs.add(unlock.trim());
                System.out.printf("[QUEST] Unlocked NPC '%s' for interaction%n", unlock);
            }
            for (String lock : step.getLockNpcs()) {
                interactableNpcs.remove(lock.trim());
                System.out.printf("[QUEST] Disabled NPC '%s' from further interaction%n", lock);
            }

            // 4. Advance step index
            currentStepIndex++;
            System.out.printf("[QUEST] Advanced Quest '%s' to step %d%n", quest.getId(), currentStepIndex);

            // 5. Check quest completion
            if (currentStepIndex > quest.getSteps().size()) {
                this.questState = QuestState.COMPLETED;
                this.interactableNpcs.clear(); // Disable all quest interactions
                recordTaskCompleted(quest.getId());
                System.out.printf("[QUEST] Quest '%s' COMPLETED!%n", quest.getId());
                if (repSystem != null && quest.getRewardRep() > 0) {
                    repSystem.addRep(quest.getRewardRep());
                    System.out.printf("[QUEST] Awarded +%d REP to player for quest completion. New REP: %d%n",
                        quest.getRewardRep(), repSystem.getRep());
                }
            }

            return feedbackMsg;
        }

        // Informational reminder dialogues do NOT advance steps or modify inventory
        return null;
    }
}
