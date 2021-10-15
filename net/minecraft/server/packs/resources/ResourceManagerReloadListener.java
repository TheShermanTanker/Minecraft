package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;

public interface ResourceManagerReloadListener extends IReloadListener {
    @Override
    default CompletableFuture<Void> reload(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, GameProfilerFiller prepareProfiler, GameProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return synchronizer.wait(Unit.INSTANCE).thenRunAsync(() -> {
            applyProfiler.startTick();
            applyProfiler.enter("listener");
            this.onResourceManagerReload(manager);
            applyProfiler.exit();
            applyProfiler.endTick();
        }, applyExecutor);
    }

    void onResourceManagerReload(IResourceManager manager);
}
