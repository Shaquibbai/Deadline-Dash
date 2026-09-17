package io.github.shaquibbai.deadlinedash.quest;

/**
 * Enum representing NPC quest marker status above NPC heads.
 * - NEW_INTERACTION: Yellow "!" indicating a new quest step interaction.
 * - REMINDER: Red "?" indicating an active reminder or pending request.
 * - QUEST_AVAILABLE: Yellow "?" indicating an available quest or uninvestigated NPC.
 * - VALID: Green "✓" indicating an investigated valid NPC.
 * - INTRUDER: Red "✕" indicating an investigated intruder NPC.
 * - NONE: No marker displayed.
 */
public enum QuestMarker {
    NONE,
    NEW_INTERACTION,
    REMINDER,
    QUEST_AVAILABLE,
    VALID,
    INTRUDER
}

