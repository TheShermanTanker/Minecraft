package net.minecraft.gametest.framework;

import javax.annotation.Nullable;

class GameTestHarnessEvent {
    @Nullable
    public final Long expectedDelay;
    public final Runnable assertion;

    private GameTestHarnessEvent(@Nullable Long duration, Runnable task) {
        this.expectedDelay = duration;
        this.assertion = task;
    }

    static GameTestHarnessEvent create(Runnable task) {
        return new GameTestHarnessEvent((Long)null, task);
    }

    static GameTestHarnessEvent create(long duration, Runnable task) {
        return new GameTestHarnessEvent(duration, task);
    }
}
