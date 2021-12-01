package net.minecraft.world.level.entity;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkCoordIntPair;

public interface EntityPersistentStorage<T> extends AutoCloseable {
    CompletableFuture<ChunkEntities<T>> loadEntities(ChunkCoordIntPair pos);

    void storeEntities(ChunkEntities<T> dataList);

    void flush(boolean sync);

    @Override
    default void close() throws IOException {
    }
}
