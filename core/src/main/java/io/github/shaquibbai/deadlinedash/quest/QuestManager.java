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
 * Manages quest step progression, dynamic NPC interactability, quest dialogue overrides,
 * inventory item rewards upon dialogue completion, and one-time REP rewards.
 */
public class QuestManager {
    private Quest quest;
    private QuestState questState = QuestState.NOT_STARTED;
    private int currentStepIndex = 1;
    private final Set<String> interactableNpcs = new HashSet<>();
    private final Set<String> managedNpcs = new HashSet<>();

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
                    String reqItem = stepVal.getString("requiredItem", null);

                    List<String> unlockNpcs = new ArrayList<>();
                    JsonValue unlockArray = stepVal.get("unlockNpcs");
                    if (unlockArray != null && unlockArray.isArray()) {
                        for (JsonValue uVal = unlockArray.child; uVal != null; uVal = uVal.next) {
                            unlockNpcs.add(uVal.asString());
                        }
                    }

                    String addItem = stepVal.getString("addItem", null);

                    steps.add(new QuestStep(sIndex, npc, dialogueId, reqItem, unlockNpcs, addItem));
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
    public void resetState() {
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
        // If NPC is part of quest management, check unlocked set; otherwise default true
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
            npc.setInteractable(interactableNpcs.contains(npc.getName()));
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

        QuestStep currentStep = quest.getStep(currentStepIndex);
        if (currentStep != null && currentStep.getNpc().equalsIgnoreCase(npcName.trim())) {
            if (currentStep.hasRequiredItem()) {
                Item item = new Item(currentStep.getRequiredItem());
                if (backpack != null && backpack.hasItem(item)) {
                    return currentStep.getDialogueId();
                } else {
                    return defaultDialogueId;
                }
            } else {
                return currentStep.getDialogueId();
            }
        }

        return defaultDialogueId;
    }

    /**
     * Evaluates dialogue completion and transitions quest step state when dialogue finishes.
     * Rewards (items/REP) are awarded strictly when dialogue finishes.
     */
    public boolean onDialogueCompleted(String dialogueId, String npcName, Backpack backpack, RepSystem repSystem) {
        if (quest == null || questState != QuestState.IN_PROGRESS || dialogueId == null || npcName == null) {
            return false;
        }

        QuestStep step = quest.getStep(currentStepIndex);
        if (step == null) return false;

        if (step.getDialogueId().equalsIgnoreCase(dialogueId.trim()) && step.getNpc().equalsIgnoreCase(npcName.trim())) {
            if (step.hasRequiredItem()) {
                Item req = new Item(step.getRequiredItem());
                if (backpack == null || !backpack.hasItem(req)) {
                    return false;
                }
            }

            // Step completed!
            if (step.hasAddItem() && backpack != null) {
                backpack.addItem(new Item(step.getAddItem()));
                System.out.printf("[QUEST] Added item '%s' to backpack upon completing dialogue '%s'%n", step.getAddItem(), dialogueId);
            }

            for (String unlock : step.getUnlockNpcs()) {
                interactableNpcs.add(unlock.trim());
                System.out.printf("[QUEST] Unlocked NPC '%s' for interaction%n", unlock);
            }

            currentStepIndex++;
            System.out.printf("[QUEST] Advanced Quest '%s' to step %d%n", quest.getId(), currentStepIndex);

            if (currentStepIndex > quest.getSteps().size()) {
                this.questState = QuestState.COMPLETED;
                System.out.printf("[QUEST] Quest '%s' COMPLETED!%n", quest.getId());
                if (repSystem != null && quest.getRewardRep() > 0) {
                    repSystem.addRep(quest.getRewardRep());
                    System.out.printf("[QUEST] Awarded +%d REP to player for quest completion. New REP: %d%n",
                        quest.getRewardRep(), repSystem.getRep());
                }
            }

            return true;
        }

        return false;
    }
}
