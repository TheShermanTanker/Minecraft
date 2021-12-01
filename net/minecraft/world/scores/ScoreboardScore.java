package net.minecraft.world.scores;

import java.util.Comparator;
import javax.annotation.Nullable;

public class ScoreboardScore {
    public static final Comparator<ScoreboardScore> SCORE_COMPARATOR = (a, b) -> {
        if (a.getScore() > b.getScore()) {
            return 1;
        } else {
            return a.getScore() < b.getScore() ? -1 : b.getPlayerName().compareToIgnoreCase(a.getPlayerName());
        }
    };
    private final Scoreboard scoreboard;
    @Nullable
    private final ScoreboardObjective objective;
    private final String owner;
    private int count;
    private boolean locked;
    private boolean forceUpdate;

    public ScoreboardScore(Scoreboard scoreboard, ScoreboardObjective objective, String playerName) {
        this.scoreboard = scoreboard;
        this.objective = objective;
        this.owner = playerName;
        this.locked = true;
        this.forceUpdate = true;
    }

    public void addScore(int amount) {
        if (this.objective.getCriteria().isReadOnly()) {
            throw new IllegalStateException("Cannot modify read-only score");
        } else {
            this.setScore(this.getScore() + amount);
        }
    }

    public void incrementScore() {
        this.addScore(1);
    }

    public int getScore() {
        return this.count;
    }

    public void reset() {
        this.setScore(0);
    }

    public void setScore(int score) {
        int i = this.count;
        this.count = score;
        if (i != score || this.forceUpdate) {
            this.forceUpdate = false;
            this.getScoreboard().handleScoreChanged(this);
        }

    }

    @Nullable
    public ScoreboardObjective getObjective() {
        return this.objective;
    }

    public String getPlayerName() {
        return this.owner;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
