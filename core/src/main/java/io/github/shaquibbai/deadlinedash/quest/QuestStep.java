package io.github.shaquibbai.deadlinedash.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data model for a single step in a quest flow.
 * Supports single or multiple required items and item removals.
 */
public class QuestStep {
    private final int stepIndex;
    private final String npc;
    private final String dialogueId;
    private final String reminderDialogueId;
    private final List<String> reminderNpcs;
    private final List<String> requiredItems;
    private final List<String> removeItems;
    private final String addItem;
    private final String addItemMessage;
    private final List<String> unlockNpcs;
    private final List<String> lockNpcs;

    public QuestStep(int stepIndex, String npc, String dialogueId, String reminderDialogueId,
                     List<String> reminderNpcs, List<String> requiredItems, List<String> removeItems,
                     String addItem, String addItemMessage, List<String> unlockNpcs, List<String> lockNpcs) {
        this.stepIndex = stepIndex;
        this.npc = npc != null ? npc.trim() : "";
        this.dialogueId = dialogueId != null ? dialogueId.trim() : "";
        this.reminderDialogueId = (reminderDialogueId != null && !reminderDialogueId.trim().isEmpty()) ? reminderDialogueId.trim() : null;
        this.reminderNpcs = reminderNpcs != null ? new ArrayList<>(reminderNpcs) : Collections.emptyList();
        
        this.requiredItems = new ArrayList<>();
        if (requiredItems != null) {
            for (String item : requiredItems) {
                if (item != null && !item.trim().isEmpty()) {
                    this.requiredItems.add(item.trim());
                }
            }
        }

        this.removeItems = new ArrayList<>();
        if (removeItems != null) {
            for (String item : removeItems) {
                if (item != null && !item.trim().isEmpty()) {
                    this.removeItems.add(item.trim());
                }
            }
        }

        this.addItem = (addItem != null && !addItem.trim().isEmpty()) ? addItem.trim() : null;
        this.addItemMessage = (addItemMessage != null && !addItemMessage.trim().isEmpty()) ? addItemMessage.trim() : null;
        this.unlockNpcs = unlockNpcs != null ? new ArrayList<>(unlockNpcs) : Collections.emptyList();
        this.lockNpcs = lockNpcs != null ? new ArrayList<>(lockNpcs) : Collections.emptyList();
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

    public String getReminderDialogueId() {
        return reminderDialogueId;
    }

    public boolean hasReminderDialogue() {
        return reminderDialogueId != null;
    }

    public List<String> getReminderNpcs() {
        return Collections.unmodifiableList(reminderNpcs);
    }

    public List<String> getRequiredItems() {
        return Collections.unmodifiableList(requiredItems);
    }

    public boolean hasRequiredItems() {
        return !requiredItems.isEmpty();
    }

    public String getRequiredItem() {
        return requiredItems.isEmpty() ? null : requiredItems.get(0);
    }

    public boolean hasRequiredItem() {
        return hasRequiredItems();
    }

    public List<String> getRemoveItems() {
        return Collections.unmodifiableList(removeItems);
    }

    public boolean hasRemoveItems() {
        return !removeItems.isEmpty();
    }

    public String getRemoveItem() {
        return removeItems.isEmpty() ? null : removeItems.get(0);
    }

    public boolean hasRemoveItem() {
        return hasRemoveItems();
    }

    public String getAddItem() {
        return addItem;
    }

    public boolean hasAddItem() {
        return addItem != null;
    }

    public String getAddItemMessage() {
        return addItemMessage;
    }

    public List<String> getUnlockNpcs() {
        return Collections.unmodifiableList(unlockNpcs);
    }

    public List<String> getLockNpcs() {
        return Collections.unmodifiableList(lockNpcs);
    }
}
