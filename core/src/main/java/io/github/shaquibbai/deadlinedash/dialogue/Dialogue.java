package io.github.shaquibbai.deadlinedash.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a conversation sequence.
 * Holds the unique dialogue ID and an ordered list of dialogue lines with speaker information.
 */
public class Dialogue {
    private final String id;
    private final List<DialogueLine> lines;

    public Dialogue(String id, List<DialogueLine> lines) {
        this.id = id != null ? id.trim() : "";
        if (lines != null) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        } else {
            this.lines = Collections.emptyList();
        }
    }

    public String getId() {
        return id;
    }

    public List<DialogueLine> getLines() {
        return lines;
    }

    public int getLineCount() {
        return lines.size();
    }

    public DialogueLine getLine(int index) {
        if (index < 0 || index >= lines.size()) {
            return new DialogueLine(DialogueSpeaker.NPC, "");
        }
        return lines.get(index);
    }

    public String getText(int index) {
        return getLine(index).getText();
    }

    public DialogueSpeaker getSpeaker(int index) {
        return getLine(index).getSpeaker();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Override
    public String toString() {
        return "Dialogue{" +
            "id='" + id + '\'' +
            ", lines=" + lines.size() +
            '}';
    }
}
