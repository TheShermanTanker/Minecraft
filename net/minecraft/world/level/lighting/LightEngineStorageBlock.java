package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;

public class LightEngineStorageBlock extends LightEngineStorage<LightEngineStorageBlock.BlockDataLayerStorageMap> {
    protected LightEngineStorageBlock(ILightAccess chunkProvider) {
        super(EnumSkyBlock.BLOCK, chunkProvider, new LightEngineStorageBlock.BlockDataLayerStorageMap(new Long2ObjectOpenHashMap<>()));
    }

    @Override
    protected int getLightValue(long blockPos) {
        long l = SectionPosition.blockToSection(blockPos);
        NibbleArray dataLayer = this.getDataLayer(l, false);
        return dataLayer == null ? 0 : dataLayer.get(SectionPosition.sectionRelative(BlockPosition.getX(blockPos)), SectionPosition.sectionRelative(BlockPosition.getY(blockPos)), SectionPosition.sectionRelative(BlockPosition.getZ(blockPos)));
    }

    protected static final class BlockDataLayerStorageMap extends LightEngineStorageArray<LightEngineStorageBlock.BlockDataLayerStorageMap> {
        public BlockDataLayerStorageMap(Long2ObjectOpenHashMap<NibbleArray> arrays) {
            super(arrays);
        }

        @Override
        public LightEngineStorageBlock.BlockDataLayerStorageMap copy() {
            return new LightEngineStorageBlock.BlockDataLayerStorageMap(this.map.clone());
        }
    }
}
