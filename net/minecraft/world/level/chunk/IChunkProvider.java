package net.minecraft.world.level.chunk;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.lighting.LightEngine;

public abstract class IChunkProvider implements ILightAccess, AutoCloseable {
    @Nullable
    public Chunk getChunkAt(int chunkX, int chunkZ, boolean create) {
        return (Chunk)this.getChunkAt(chunkX, chunkZ, ChunkStatus.FULL, create);
    }

    @Nullable
    public Chunk getChunkNow(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, false);
    }

    @Nullable
    @Override
    public IBlockAccess getChunkForLighting(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }

    public boolean isLoaded(int x, int z) {
        return this.getChunkAt(x, z, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public abstract IChunkAccess getChunkAt(int x, int z, ChunkStatus leastStatus, boolean create);

    public abstract void tick(BooleanSupplier booleanSupplier);

    public abstract String getName();

    public abstract int getLoadedChunksCount();

    @Override
    public void close() throws IOException {
    }

    public abstract LightEngine getLightEngine();

    public void setSpawnSettings(boolean spawnMonsters, boolean spawnAnimals) {
    }

    public void updateChunkForced(ChunkCoordIntPair pos, boolean forced) {
    }
}
