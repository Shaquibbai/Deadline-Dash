package io.github.shaquibbai.deadlinedash.quest;

/**
 * Enum representing NPC quest marker status above NPC heads.
 * - NEW_INTERACTION: Yellow "!" indicating a new quest step interaction.
 * - REMINDER: Red "?" indicating an active reminder or pending request.
 * - NONE: No marker displayed.
 */
public enum QuestMarker {
    NONE,
    NEW_INTERACTION,
    REMINDER
}
