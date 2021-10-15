package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.NibbleArray;

public interface LightEngineLayerEventListener extends ILightEngine {
    @Nullable
    NibbleArray getDataLayerData(SectionPosition pos);

    int getLightValue(BlockPosition pos);

    public static enum Void implements LightEngineLayerEventListener {
        INSTANCE;

        @Nullable
        @Override
        public NibbleArray getDataLayerData(SectionPosition pos) {
            return null;
        }

        @Override
        public int getLightValue(BlockPosition pos) {
            return 0;
        }

        @Override
        public void checkBlock(BlockPosition pos) {
        }

        @Override
        public void onBlockEmissionIncrease(BlockPosition pos, int level) {
        }

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runUpdates(int i, boolean bl, boolean bl2) {
            return i;
        }

        @Override
        public void updateSectionStatus(SectionPosition pos, boolean notReady) {
        }

        @Override
        public void enableLightSources(ChunkCoordIntPair chunkPos, boolean bl) {
        }
    }
}
