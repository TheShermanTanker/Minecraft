package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.GameProfilerFiller;

public abstract class ResourceDataAbstract<T> implements IReloadListener {
    @Override
    public final CompletableFuture<Void> reload(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, GameProfilerFiller prepareProfiler, GameProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            return this.prepare(manager, prepareProfiler);
        }, prepareExecutor).thenCompose(synchronizer::wait).thenAcceptAsync((object) -> {
            this.apply(object, manager, applyProfiler);
        }, applyExecutor);
    }

    protected abstract T prepare(IResourceManager manager, GameProfilerFiller profiler);

    protected abstract void apply(T prepared, IResourceManager manager, GameProfilerFiller profiler);
}
