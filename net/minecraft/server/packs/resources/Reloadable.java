package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SystemUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerDisabled;

public class Reloadable<S> implements IReloadable {
    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    protected final IResourceManager resourceManager;
    protected final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
    protected final CompletableFuture<List<S>> allDone;
    final Set<IReloadListener> preparingListeners;
    private final int listenerCount;
    private int startedReloads;
    private int finishedReloads;
    private final AtomicInteger startedTaskCounter = new AtomicInteger();
    private final AtomicInteger doneTaskCounter = new AtomicInteger();

    public static Reloadable<Void> of(IResourceManager manager, List<IReloadListener> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage) {
        return new Reloadable<>(prepareExecutor, applyExecutor, manager, reloaders, (synchronizer, resourceManager, reloader, prepare, apply) -> {
            return reloader.reload(synchronizer, resourceManager, GameProfilerDisabled.INSTANCE, GameProfilerDisabled.INSTANCE, prepareExecutor, apply);
        }, initialStage);
    }

    protected Reloadable(Executor prepareExecutor, Executor applyExecutor, IResourceManager manager, List<IReloadListener> reloaders, Reloadable.StateFactory<S> factory, CompletableFuture<Unit> initialStage) {
        this.resourceManager = manager;
        this.listenerCount = reloaders.size();
        this.startedTaskCounter.incrementAndGet();
        initialStage.thenRun(this.doneTaskCounter::incrementAndGet);
        List<CompletableFuture<S>> list = Lists.newArrayList();
        CompletableFuture<?> completableFuture = initialStage;
        this.preparingListeners = Sets.newHashSet(reloaders);

        for(final IReloadListener preparableReloadListener : reloaders) {
            final CompletableFuture<?> completableFuture2 = completableFuture;
            CompletableFuture<S> completableFuture3 = factory.create(new IReloadListener.PreparationBarrier() {
                @Override
                public <T> CompletableFuture<T> wait(T preparedObject) {
                    applyExecutor.execute(() -> {
                        Reloadable.this.preparingListeners.remove(preparableReloadListener);
                        if (Reloadable.this.preparingListeners.isEmpty()) {
                            Reloadable.this.allPreparations.complete(Unit.INSTANCE);
                        }

                    });
                    return Reloadable.this.allPreparations.thenCombine(completableFuture2, (unit, object2) -> {
                        return preparedObject;
                    });
                }
            }, manager, preparableReloadListener, (preparation) -> {
                this.startedTaskCounter.incrementAndGet();
                prepareExecutor.execute(() -> {
                    preparation.run();
                    this.doneTaskCounter.incrementAndGet();
                });
            }, (application) -> {
                ++this.startedReloads;
                applyExecutor.execute(() -> {
                    application.run();
                    ++this.finishedReloads;
                });
            });
            list.add(completableFuture3);
            completableFuture = completableFuture3;
        }

        this.allDone = SystemUtils.sequenceFailFast(list);
    }

    @Override
    public CompletableFuture<Unit> done() {
        return this.allDone.thenApply((results) -> {
            return Unit.INSTANCE;
        });
    }

    @Override
    public float getActualProgress() {
        int i = this.listenerCount - this.preparingListeners.size();
        float f = (float)(this.doneTaskCounter.get() * 2 + this.finishedReloads * 2 + i * 1);
        float g = (float)(this.startedTaskCounter.get() * 2 + this.startedReloads * 2 + this.listenerCount * 1);
        return f / g;
    }

    @Override
    public boolean isDone() {
        return this.allDone.isDone();
    }

    @Override
    public void checkExceptions() {
        if (this.allDone.isCompletedExceptionally()) {
            this.allDone.join();
        }

    }

    protected interface StateFactory<S> {
        CompletableFuture<S> create(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, IReloadListener reloader, Executor prepareExecutor, Executor applyExecutor);
    }
}
