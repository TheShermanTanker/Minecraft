package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class BulkSectionAccess implements AutoCloseable {
    private final GeneratorAccess level;
    private final Long2ObjectMap<ChunkSection> acquiredSections = new Long2ObjectOpenHashMap<>();
    @Nullable
    private ChunkSection lastSection;
    private long lastSectionKey;

    public BulkSectionAccess(GeneratorAccess world) {
        this.level = world;
    }

    @Nullable
    public ChunkSection getSection(BlockPosition pos) {
        int i = this.level.getSectionIndex(pos.getY());
        if (i >= 0 && i < this.level.getSectionsCount()) {
            long l = SectionPosition.asLong(pos);
            if (this.lastSection == null || this.lastSectionKey != l) {
                this.lastSection = this.acquiredSections.computeIfAbsent(l, (lx) -> {
                    IChunkAccess chunkAccess = this.level.getChunkAt(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
                    ChunkSection levelChunkSection = chunkAccess.getSection(i);
                    levelChunkSection.acquire();
                    return levelChunkSection;
                });
                this.lastSectionKey = l;
            }

            return this.lastSection;
        } else {
            return null;
        }
    }

    public IBlockData getBlockState(BlockPosition pos) {
        ChunkSection levelChunkSection = this.getSection(pos);
        if (levelChunkSection == null) {
            return Blocks.AIR.getBlockData();
        } else {
            int i = SectionPosition.sectionRelative(pos.getX());
            int j = SectionPosition.sectionRelative(pos.getY());
            int k = SectionPosition.sectionRelative(pos.getZ());
            return levelChunkSection.getType(i, j, k);
        }
    }

    @Override
    public void close() {
        for(ChunkSection levelChunkSection : this.acquiredSections.values()) {
            levelChunkSection.release();
        }

    }
}
