package io.github.shaquibbai.deadlinedash.rep;

/**
 * Dedicated REP/progression system for Deadline Dash.
 * Tracks player Reputation points (REP) starting at 20.
 * REP is clamped to a minimum value of 0.
 */
public class RepSystem {
    private static final int INITIAL_REP = 20;
    private int rep;

    public RepSystem() {
        this.rep = INITIAL_REP;
    }

    public RepSystem(int startingRep) {
        this.rep = Math.max(0, startingRep);
    }

    /**
     * Returns current REP integer value.
     */
    public int getRep() {
        return rep;
    }

    /**
     * Adds the specified positive amount to current REP.
     */
    public void addRep(int amount) {
        if (amount <= 0) return;
        this.rep += amount;
    }

    /**
     * Removes the specified amount from current REP.
     * Guarantees REP never drops below 0.
     */
    public void removeRep(int amount) {
        if (amount <= 0) return;
        this.rep = Math.max(0, this.rep - amount);
    }

    /**
     * Resets REP to initial starting value (20).
     */
    public void reset() {
        this.rep = INITIAL_REP;
    }
}
