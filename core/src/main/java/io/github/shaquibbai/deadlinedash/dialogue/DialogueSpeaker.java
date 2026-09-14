package io.github.shaquibbai.deadlinedash.dialogue;

/**
 * Enumeration representing the speaker of a dialogue line.
 */
public enum DialogueSpeaker {
    NPC,
    PLAYER;

    public static DialogueSpeaker fromString(String value) {
        if (value != null && value.trim().equalsIgnoreCase("player")) {
            return PLAYER;
        }
        return NPC;
    }
}
