package io.github.shaquibbai.deadlinedash.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data model for a single step in a quest flow.
 */
public class QuestStep {
    private final int stepIndex;
    private final String npc;
    private final String dialogueId;
    private final String requiredItem;
    private final List<String> unlockNpcs;
    private final String addItem;

    public QuestStep(int stepIndex, String npc, String dialogueId, String requiredItem, List<String> unlockNpcs, String addItem) {
        this.stepIndex = stepIndex;
        this.npc = npc != null ? npc.trim() : "";
        this.dialogueId = dialogueId != null ? dialogueId.trim() : "";
        this.requiredItem = (requiredItem != null && !requiredItem.trim().isEmpty()) ? requiredItem.trim() : null;
        this.unlockNpcs = unlockNpcs != null ? new ArrayList<>(unlockNpcs) : Collections.emptyList();
        this.addItem = (addItem != null && !addItem.trim().isEmpty()) ? addItem.trim() : null;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public String getNpc() {
        return npc;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public String getRequiredItem() {
        return requiredItem;
    }

    public boolean hasRequiredItem() {
        return requiredItem != null;
    }

    public List<String> getUnlockNpcs() {
        return Collections.unmodifiableList(unlockNpcs);
    }

    public String getAddItem() {
        return addItem;
    }

    public boolean hasAddItem() {
        return addItem != null;
    }
}
