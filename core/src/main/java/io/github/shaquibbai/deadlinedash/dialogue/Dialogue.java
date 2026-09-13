package io.github.shaquibbai.deadlinedash.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a single conversation tree/dialogue sequence.
 * Holds the unique dialogue ID and an ordered list of text lines.
 */
public class Dialogue {
    private final String id;
    private final List<String> lines;

    public Dialogue(String id, List<String> lines) {
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

    public List<String> getLines() {
        return lines;
    }

    public int getLineCount() {
        return lines.size();
    }

    public String getLine(int index) {
        if (index < 0 || index >= lines.size()) {
            return "";
        }
        return lines.get(index);
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
