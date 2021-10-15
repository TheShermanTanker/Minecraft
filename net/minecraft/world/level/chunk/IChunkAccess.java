package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidType;
import org.apache.logging.log4j.LogManager;

public interface IChunkAccess extends IBlockAccess, IStructureAccess {
    default GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return GameEventDispatcher.NOOP;
    }

    @Nullable
    IBlockData setType(BlockPosition pos, IBlockData state, boolean moved);

    void setTileEntity(TileEntity blockEntity);

    void addEntity(Entity entity);

    @Nullable
    default ChunkSection getHighestSection() {
        ChunkSection[] levelChunkSections = this.getSections();

        for(int i = levelChunkSections.length - 1; i >= 0; --i) {
            ChunkSection levelChunkSection = levelChunkSections[i];
            if (!ChunkSection.isEmpty(levelChunkSection)) {
                return levelChunkSection;
            }
        }

        return null;
    }

    default int getHighestSectionPosition() {
        ChunkSection levelChunkSection = this.getHighestSection();
        return levelChunkSection == null ? this.getMinBuildHeight() : levelChunkSection.getYPosition();
    }

    Set<BlockPosition> getBlockEntitiesPos();

    ChunkSection[] getSections();

    default ChunkSection getOrCreateSection(int yIndex) {
        ChunkSection[] levelChunkSections = this.getSections();
        if (levelChunkSections[yIndex] == Chunk.EMPTY_SECTION) {
            levelChunkSections[yIndex] = new ChunkSection(this.getSectionYFromSectionIndex(yIndex));
        }

        return levelChunkSections[yIndex];
    }

    Collection<Entry<HeightMap.Type, HeightMap>> getHeightmaps();

    default void setHeightmap(HeightMap.Type type, long[] heightmap) {
        this.getOrCreateHeightmapUnprimed(type).setRawData(this, type, heightmap);
    }

    HeightMap getOrCreateHeightmapUnprimed(HeightMap.Type type);

    int getHighestBlock(HeightMap.Type type, int x, int z);

    BlockPosition getHeighestPosition(HeightMap.Type types);

    ChunkCoordIntPair getPos();

    Map<StructureGenerator<?>, StructureStart<?>> getAllStarts();

    void setAllStarts(Map<StructureGenerator<?>, StructureStart<?>> structureStarts);

    default boolean isYSpaceEmpty(int lowerHeight, int upperHeight) {
        if (lowerHeight < this.getMinBuildHeight()) {
            lowerHeight = this.getMinBuildHeight();
        }

        if (upperHeight >= this.getMaxBuildHeight()) {
            upperHeight = this.getMaxBuildHeight() - 1;
        }

        for(int i = lowerHeight; i <= upperHeight; i += 16) {
            if (!ChunkSection.isEmpty(this.getSections()[this.getSectionIndex(i)])) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    BiomeStorage getBiomeIndex();

    void setNeedsSaving(boolean shouldSave);

    boolean isNeedsSaving();

    ChunkStatus getChunkStatus();

    void removeTileEntity(BlockPosition pos);

    default void markPosForPostprocessing(BlockPosition pos) {
        LogManager.getLogger().warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", (Object)pos);
    }

    ShortList[] getPostProcessing();

    default void addPackedPostProcess(short packedPos, int index) {
        getOrCreateOffsetList(this.getPostProcessing(), index).add(packedPos);
    }

    default void setBlockEntityNbt(NBTTagCompound nbt) {
        LogManager.getLogger().warn("Trying to set a BlockEntity, but this operation is not supported.");
    }

    @Nullable
    NBTTagCompound getBlockEntityNbt(BlockPosition pos);

    @Nullable
    NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos);

    Stream<BlockPosition> getLights();

    TickList<Block> getBlockTicks();

    TickList<FluidType> getLiquidTicks();

    ChunkConverter getUpgradeData();

    void setInhabitedTime(long inhabitedTime);

    long getInhabitedTime();

    static ShortList getOrCreateOffsetList(ShortList[] lists, int index) {
        if (lists[index] == null) {
            lists[index] = new ShortArrayList();
        }

        return lists[index];
    }

    boolean isLightCorrect();

    void setLightCorrect(boolean lightOn);
}
