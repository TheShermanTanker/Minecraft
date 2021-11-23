package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;
import net.minecraft.util.profiling.metrics.IProfilerMeasured;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IAsyncTaskHandler<R extends Runnable> implements IProfilerMeasured, Mailbox<R>, Executor {
    private final String name;
    private static final Logger LOGGER = LogManager.getLogger();
    private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
    private int blockingCount;

    protected IAsyncTaskHandler(String name) {
        this.name = name;
        MetricsRegistry.INSTANCE.add(this);
    }

    protected abstract R postToMainThread(Runnable runnable);

    protected abstract boolean canExecute(R task);

    public boolean isMainThread() {
        return Thread.currentThread() == this.getThread();
    }

    protected abstract Thread getThread();

    protected boolean scheduleExecutables() {
        return !this.isMainThread();
    }

    public int getPendingTasksCount() {
        return this.pendingRunnables.size();
    }

    @Override
    public String name() {
        return this.name;
    }

    public <V> CompletableFuture<V> submit(Supplier<V> task) {
        return this.scheduleExecutables() ? CompletableFuture.supplyAsync(task, this) : CompletableFuture.completedFuture(task.get());
    }

    private CompletableFuture<Void> executeFuture(Runnable runnable) {
        return CompletableFuture.supplyAsync(() -> {
            runnable.run();
            return null;
        }, this);
    }

    public CompletableFuture<Void> submit(Runnable task) {
        if (this.scheduleExecutables()) {
            return this.executeFuture(task);
        } else {
            task.run();
            return CompletableFuture.completedFuture((Void)null);
        }
    }

    public void executeSync(Runnable runnable) {
        if (!this.isMainThread()) {
            this.executeFuture(runnable).join();
        } else {
            runnable.run();
        }

    }

    @Override
    public void tell(R runnable) {
        this.pendingRunnables.add(runnable);
        LockSupport.unpark(this.getThread());
    }

    @Override
    public void execute(Runnable runnable) {
        if (this.scheduleExecutables()) {
            this.tell(this.postToMainThread(runnable));
        } else {
            runnable.run();
        }

    }

    protected void dropAllTasks() {
        this.pendingRunnables.clear();
    }

    public void executeAll() {
        while(this.executeNext()) {
        }

    }

    public boolean executeNext() {
        R runnable = this.pendingRunnables.peek();
        if (runnable == null) {
            return false;
        } else if (this.blockingCount == 0 && !this.canExecute(runnable)) {
            return false;
        } else {
            this.executeTask(this.pendingRunnables.remove());
            return true;
        }
    }

    public void awaitTasks(BooleanSupplier stopCondition) {
        ++this.blockingCount;

        try {
            while(!stopCondition.getAsBoolean()) {
                if (!this.executeNext()) {
                    this.waitForTasks();
                }
            }
        } finally {
            --this.blockingCount;
        }

    }

    protected void waitForTasks() {
        Thread.yield();
        LockSupport.parkNanos("waiting for tasks", 100000L);
    }

    protected void executeTask(R task) {
        try {
            task.run();
        } catch (Exception var3) {
            LOGGER.fatal("Error executing task on {}", this.name(), var3);
        }

    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of(MetricSampler.create(this.name + "-pending-tasks", EnumMetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
    }
}
