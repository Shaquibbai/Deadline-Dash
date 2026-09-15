package io.github.shaquibbai.deadlinedash.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data model representing a full Quest definition.
 */
public class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final int rewardRep;
    private final List<String> initialInteractableNpcs;
    private final List<QuestStep> steps;

    public Quest(String id, String name, String description, int rewardRep, List<String> initialInteractableNpcs, List<QuestStep> steps) {
        this.id = id != null ? id.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.rewardRep = Math.max(0, rewardRep);
        this.initialInteractableNpcs = initialInteractableNpcs != null ? new ArrayList<>(initialInteractableNpcs) : Collections.emptyList();
        this.steps = steps != null ? new ArrayList<>(steps) : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRewardRep() {
        return rewardRep;
    }

    public List<String> getInitialInteractableNpcs() {
        return Collections.unmodifiableList(initialInteractableNpcs);
    }

    public List<QuestStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public QuestStep getStep(int stepIndex) {
        for (QuestStep step : steps) {
            if (step.getStepIndex() == stepIndex) {
                return step;
            }
        }
        return null;
    }

    public int TotalSteps() {
        return steps.size();
    }
}
