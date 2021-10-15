package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.GameProfilerFiller;

public interface IReloadListener {
    CompletableFuture<Void> reload(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, GameProfilerFiller prepareProfiler, GameProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor);

    default String getName() {
        return this.getClass().getSimpleName();
    }

    public interface PreparationBarrier {
        <T> CompletableFuture<T> wait(T preparedObject);
    }
}
