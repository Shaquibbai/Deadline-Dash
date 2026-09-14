package io.github.shaquibbai.deadlinedash.dialogue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.shaquibbai.deadlinedash.npc.NPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State manager and data registry for in-game dialogues.
 * Handles loading dialogue data, initiating conversations with NPCs, tracking
 * the current line state across NPC and player speakers, and advancing or ending dialogues.
 */
public class DialogueManager {
    private static final String DEFAULT_DIALOGUES_PATH = "dialogue/dialogues.json";

    private final Map<String, Dialogue> dialogues = new HashMap<>();

    private boolean active = false;
    private NPC currentNpc = null;
    private Dialogue currentDialogue = null;
    private int currentLineIndex = 0;

    public DialogueManager() {
        loadDefaultDialogues();
    }

    public DialogueManager(String jsonPath) {
        loadDialogues(jsonPath);
    }

    /**
     * Attempts to load dialogues from the default assets path if Gdx filesystem is available.
     */
    public void loadDefaultDialogues() {
        if (Gdx.files != null) {
            loadDialogues(DEFAULT_DIALOGUES_PATH);
        }
    }

    /**
     * Loads dialogues from a file path using LibGDX file handling.
     */
    public void loadDialogues(String path) {
        if (Gdx.files == null) {
            System.err.println("[DIALOGUE] Gdx.files is not initialized. Cannot load dialogues from path.");
            return;
        }

        try {
            FileHandle handle = Gdx.files.internal(path);
            if (!handle.exists()) {
                System.err.printf("[DIALOGUE] ERROR: Dialogues file not found at '%s'.%n", path);
                return;
            }
            String content = handle.readString("UTF-8");
            loadDialoguesFromString(content);
        } catch (Exception e) {
            System.err.printf("[DIALOGUE] ERROR: Failed loading dialogues from '%s': %s%n", path, e.getMessage());
        }
    }

    /**
     * Parses JSON string content and populates the dialogue registry.
     * Supports line objects with "speaker" and "text" fields as well as plain string fallbacks.
     */
    public void loadDialoguesFromString(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return;
        }

        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(jsonContent);
            if (root == null) return;

            for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                String id = entry.name;
                List<DialogueLine> lines = new ArrayList<>();
                JsonValue linesArray = entry.get("lines");
                if (linesArray != null && linesArray.isArray()) {
                    for (JsonValue lineVal = linesArray.child; lineVal != null; lineVal = lineVal.next) {
                        if (lineVal.isObject()) {
                            String speakerStr = lineVal.getString("speaker", "npc");
                            String textStr = lineVal.getString("text", "");
                            lines.add(new DialogueLine(speakerStr, textStr));
                        } else if (lineVal.isString()) {
                            lines.add(new DialogueLine(DialogueSpeaker.NPC, lineVal.asString()));
                        }
                    }
                }
                dialogues.put(id, new Dialogue(id, lines));
            }
            System.out.printf("[DIALOGUE] Successfully loaded %d dialogues.%n", dialogues.size());
        } catch (Exception e) {
            System.err.printf("[DIALOGUE] ERROR: Failed parsing dialogue JSON: %s%n", e.getMessage());
        }
    }

    /**
     * Starts conversation with the specified NPC.
     *
     * @param npc the target NPC
     * @return true if dialogue successfully started, false otherwise
     */
    public boolean startDialogue(NPC npc) {
        if (npc == null) {
            return false;
        }

        if (!npc.getConfig().isInteractable()) {
            return false;
        }

        String dialogueId = npc.getConfig().getDialogue();
        if (dialogueId == null || dialogueId.trim().isEmpty() || "NONE".equalsIgnoreCase(dialogueId.trim())) {
            return false;
        }

        Dialogue dialogue = dialogues.get(dialogueId.trim());
        if (dialogue == null || dialogue.isEmpty()) {
            System.err.printf("[DIALOGUE] WARNING: Dialogue '%s' not found for NPC '%s'.%n", dialogueId, npc.getName());
            return false;
        }

        this.currentNpc = npc;
        this.currentDialogue = dialogue;
        this.currentLineIndex = 0;
        this.active = true;

        System.out.printf("[DIALOGUE] Started dialogue '%s' with speaker '%s'%n", dialogue.getId(), getCurrentSpeaker());
        return true;
    }

    /**
     * Advances to the next line in the current dialogue or closes it after the last line.
     */
    public void advanceDialogue() {
        if (!active || currentDialogue == null) {
            return;
        }

        currentLineIndex++;
        if (currentLineIndex >= currentDialogue.getLineCount()) {
            endDialogue();
        }
    }

    /**
     * Closes the active dialogue and resets conversational state.
     */
    public void endDialogue() {
        if (active) {
            System.out.printf("[DIALOGUE] Finished dialogue with '%s'%n", getCurrentSpeaker());
        }
        this.active = false;
        this.currentNpc = null;
        this.currentDialogue = null;
        this.currentLineIndex = 0;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Returns the name of the active speaker.
     * Displays the NPC's Tiled name for NPC lines, and "Player" for player lines.
     */
    public String getCurrentSpeaker() {
        if (!active || currentDialogue == null) {
            return "";
        }
        DialogueLine line = currentDialogue.getLine(currentLineIndex);
        if (line != null && line.isPlayer()) {
            return "Player";
        }
        if (currentNpc != null) {
            return currentNpc.getName();
        }
        return "";
    }

    public DialogueSpeaker getCurrentSpeakerType() {
        if (!active || currentDialogue == null) {
            return DialogueSpeaker.NPC;
        }
        DialogueLine line = currentDialogue.getLine(currentLineIndex);
        return line != null ? line.getSpeaker() : DialogueSpeaker.NPC;
    }

    public String getCurrentLine() {
        if (active && currentDialogue != null) {
            return currentDialogue.getText(currentLineIndex);
        }
        return "";
    }

    public DialogueLine getCurrentDialogueLine() {
        if (active && currentDialogue != null) {
            return currentDialogue.getLine(currentLineIndex);
        }
        return null;
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }

    public int getTotalLines() {
        if (currentDialogue != null) {
            return currentDialogue.getLineCount();
        }
        return 0;
    }

    public boolean isLastLine() {
        return active && currentDialogue != null && currentLineIndex >= currentDialogue.getLineCount() - 1;
    }

    public Dialogue getDialogue(String id) {
        return dialogues.get(id);
    }

    public NPC getCurrentNpc() {
        return currentNpc;
    }

    public Dialogue getCurrentDialogue() {
        return currentDialogue;
    }

    public void clear() {
        endDialogue();
        dialogues.clear();
    }
}
