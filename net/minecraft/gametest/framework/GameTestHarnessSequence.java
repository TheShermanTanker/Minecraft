package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class GameTestHarnessSequence {
    final GameTestHarnessInfo parent;
    private final List<GameTestHarnessEvent> events = Lists.newArrayList();
    private long lastTick;

    GameTestHarnessSequence(GameTestHarnessInfo gameTest) {
        this.parent = gameTest;
        this.lastTick = gameTest.getTick();
    }

    public GameTestHarnessSequence thenWaitUntil(Runnable task) {
        this.events.add(GameTestHarnessEvent.create(task));
        return this;
    }

    public GameTestHarnessSequence thenWaitUntil(long duration, Runnable task) {
        this.events.add(GameTestHarnessEvent.create(duration, task));
        return this;
    }

    public GameTestHarnessSequence thenIdle(int i) {
        return this.thenExecuteAfter(i, () -> {
        });
    }

    public GameTestHarnessSequence thenExecute(Runnable task) {
        this.events.add(GameTestHarnessEvent.create(() -> {
            this.executeWithoutFail(task);
        }));
        return this;
    }

    public GameTestHarnessSequence thenExecuteAfter(int delay, Runnable task) {
        this.events.add(GameTestHarnessEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + (long)delay) {
                throw new GameTestHarnessAssertion("Waiting");
            } else {
                this.executeWithoutFail(task);
            }
        }));
        return this;
    }

    public GameTestHarnessSequence thenExecuteFor(int i, Runnable task) {
        this.events.add(GameTestHarnessEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + (long)i) {
                this.executeWithoutFail(task);
                throw new GameTestHarnessAssertion("Waiting");
            }
        }));
        return this;
    }

    public void thenSucceed() {
        this.events.add(GameTestHarnessEvent.create(this.parent::succeed));
    }

    public void thenFail(Supplier<Exception> supplier) {
        this.events.add(GameTestHarnessEvent.create(() -> {
            this.parent.fail(supplier.get());
        }));
    }

    public GameTestHarnessSequence.Condition thenTrigger() {
        GameTestHarnessSequence.Condition condition = new GameTestHarnessSequence.Condition();
        this.events.add(GameTestHarnessEvent.create(() -> {
            condition.trigger(this.parent.getTick());
        }));
        return condition;
    }

    public void tickAndContinue(long tick) {
        try {
            this.tick(tick);
        } catch (GameTestHarnessAssertion var4) {
        }

    }

    public void tickAndFailIfNotComplete(long tick) {
        try {
            this.tick(tick);
        } catch (GameTestHarnessAssertion var4) {
            this.parent.fail(var4);
        }

    }

    private void executeWithoutFail(Runnable task) {
        try {
            task.run();
        } catch (GameTestHarnessAssertion var3) {
            this.parent.fail(var3);
        }

    }

    private void tick(long tick) {
        Iterator<GameTestHarnessEvent> iterator = this.events.iterator();

        while(iterator.hasNext()) {
            GameTestHarnessEvent gameTestEvent = iterator.next();
            gameTestEvent.assertion.run();
            iterator.remove();
            long l = tick - this.lastTick;
            long m = this.lastTick;
            this.lastTick = tick;
            if (gameTestEvent.expectedDelay != null && gameTestEvent.expectedDelay != l) {
                this.parent.fail(new GameTestHarnessAssertion("Succeeded in invalid tick: expected " + (m + gameTestEvent.expectedDelay) + ", but current tick is " + tick));
                break;
            }
        }

    }

    public class Condition {
        private static final long NOT_TRIGGERED = -1L;
        private long triggerTime = -1L;

        void trigger(long tick) {
            if (this.triggerTime != -1L) {
                throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
            } else {
                this.triggerTime = tick;
            }
        }

        public void assertTriggeredThisTick() {
            long l = GameTestHarnessSequence.this.parent.getTick();
            if (this.triggerTime != l) {
                if (this.triggerTime == -1L) {
                    throw new GameTestHarnessAssertion("Condition not triggered (t=" + l + ")");
                } else {
                    throw new GameTestHarnessAssertion("Condition triggered at " + this.triggerTime + ", (t=" + l + ")");
                }
            }
        }
    }
}
