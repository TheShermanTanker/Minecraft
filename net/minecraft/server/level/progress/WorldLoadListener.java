package net.minecraft.server.level.progress;

import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ChunkStatus;

public interface WorldLoadListener {
    void updateSpawnPos(ChunkCoordIntPair spawnPos);

    void onStatusChange(ChunkCoordIntPair pos, @Nullable ChunkStatus status);

    void start();

    void stop();
}
