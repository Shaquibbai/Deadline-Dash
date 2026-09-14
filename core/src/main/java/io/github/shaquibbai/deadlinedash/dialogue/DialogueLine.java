package io.github.shaquibbai.deadlinedash.dialogue;

/**
 * Model representing a single line of dialogue with a designated speaker.
 */
public class DialogueLine {
    private final DialogueSpeaker speaker;
    private final String text;

    public DialogueLine(DialogueSpeaker speaker, String text) {
        this.speaker = speaker != null ? speaker : DialogueSpeaker.NPC;
        this.text = text != null ? text : "";
    }

    public DialogueLine(String speaker, String text) {
        this(DialogueSpeaker.fromString(speaker), text);
    }

    public DialogueSpeaker getSpeaker() {
        return speaker;
    }

    public boolean isPlayer() {
        return speaker == DialogueSpeaker.PLAYER;
    }

    public boolean isNpc() {
        return speaker == DialogueSpeaker.NPC;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "DialogueLine{" +
            "speaker=" + speaker +
            ", text='" + text + '\'' +
            '}';
    }
}
