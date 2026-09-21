package io.github.shaquibbai.deadlinedash;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.shaquibbai.deadlinedash.screen.OpeningScreen;

/** Main game application entry point. */
public class DeadlineDash extends Game {
    private SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new OpeningScreen(this));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    private boolean openingDialogueSeen = false;

    public boolean hasOpeningDialogueBeenSeen() {
        return openingDialogueSeen;
    }

    public void setOpeningDialogueSeen(boolean seen) {
        this.openingDialogueSeen = seen;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
    }
}

