package net.minecraft.server.packs.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.IResourcePack;
import net.minecraft.util.Unit;

public interface IReloadableResourceManager extends IResourceManager, AutoCloseable {
    default CompletableFuture<Unit> reload(Executor prepareExecutor, Executor applyExecutor, List<IResourcePack> packs, CompletableFuture<Unit> initialStage) {
        return this.createReload(prepareExecutor, applyExecutor, initialStage, packs).done();
    }

    IReloadable createReload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<IResourcePack> packs);

    void registerReloadListener(IReloadListener reloader);

    @Override
    void close();
}
