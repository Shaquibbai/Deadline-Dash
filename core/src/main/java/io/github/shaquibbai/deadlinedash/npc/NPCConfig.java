package io.github.shaquibbai.deadlinedash.npc;

/**
 * Immutable configuration holder for NPC properties defined in Tiled maps.
 */
public class NPCConfig {
    private final String name;
    private final String model;
    private final String dialogue;
    private final String quest;
    private final boolean idleAnimation;
    private final boolean interactable;
    private final float interactionRange;

    public NPCConfig(String name, String model, String dialogue, String quest,
                     boolean idleAnimation, boolean interactable, float interactionRange) {
        this.name = name != null ? name : "";
        this.model = model != null ? model : "";
        this.dialogue = dialogue != null ? dialogue : "NONE";
        this.quest = quest != null ? quest : "NONE";
        this.idleAnimation = idleAnimation;
        this.interactable = interactable;
        this.interactionRange = Math.max(0f, interactionRange);
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public String getDialogue() {
        return dialogue;
    }

    public String getQuest() {
        return quest;
    }

    public boolean isIdleAnimation() {
        return idleAnimation;
    }

    public boolean isInteractable() {
        return interactable;
    }

    public float getInteractionRange() {
        return interactionRange;
    }

    @Override
    public String toString() {
        return "NPCConfig{" +
            "name='" + name + '\'' +
            ", model='" + model + '\'' +
            ", dialogue='" + dialogue + '\'' +
            ", quest='" + quest + '\'' +
            ", idleAnimation=" + idleAnimation +
            ", interactable=" + interactable +
            ", interactionRange=" + interactionRange +
            '}';
    }
}
