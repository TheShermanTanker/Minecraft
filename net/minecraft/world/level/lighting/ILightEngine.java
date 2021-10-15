package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;

public interface ILightEngine {
    void checkBlock(BlockPosition pos);

    void onBlockEmissionIncrease(BlockPosition pos, int level);

    boolean hasLightWork();

    int runUpdates(int i, boolean bl, boolean bl2);

    default void updateSectionStatus(BlockPosition pos, boolean notReady) {
        this.updateSectionStatus(SectionPosition.of(pos), notReady);
    }

    void updateSectionStatus(SectionPosition pos, boolean notReady);

    void enableLightSources(ChunkCoordIntPair chunkPos, boolean bl);
}
