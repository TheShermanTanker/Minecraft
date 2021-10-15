package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickListPriority;
import net.minecraft.world.level.chunk.storage.ChunkRegionLoader;

public class ProtoChunkTickList<T> implements TickList<T> {
    protected final Predicate<T> ignore;
    private final ChunkCoordIntPair chunkPos;
    private final ShortList[] toBeTicked;
    private IWorldHeightAccess levelHeightAccessor;

    public ProtoChunkTickList(Predicate<T> shouldExclude, ChunkCoordIntPair pos, IWorldHeightAccess world) {
        this(shouldExclude, pos, new NBTTagList(), world);
    }

    public ProtoChunkTickList(Predicate<T> shouldExclude, ChunkCoordIntPair pos, NBTTagList nbtList, IWorldHeightAccess world) {
        this.ignore = shouldExclude;
        this.chunkPos = pos;
        this.levelHeightAccessor = world;
        this.toBeTicked = new ShortList[world.getSectionsCount()];

        for(int i = 0; i < nbtList.size(); ++i) {
            NBTTagList listTag = nbtList.getList(i);

            for(int j = 0; j < listTag.size(); ++j) {
                IChunkAccess.getOrCreateOffsetList(this.toBeTicked, i).add(listTag.getShort(j));
            }
        }

    }

    public NBTTagList save() {
        return ChunkRegionLoader.packOffsets(this.toBeTicked);
    }

    public void copyOut(TickList<T> scheduler, Function<BlockPosition, T> dataMapper) {
        for(int i = 0; i < this.toBeTicked.length; ++i) {
            if (this.toBeTicked[i] != null) {
                for(Short short_ : this.toBeTicked[i]) {
                    BlockPosition blockPos = ProtoChunk.unpackOffsetCoordinates(short_, this.levelHeightAccessor.getSectionYFromSectionIndex(i), this.chunkPos);
                    scheduler.scheduleTick(blockPos, dataMapper.apply(blockPos), 0);
                }

                this.toBeTicked[i].clear();
            }
        }

    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T object) {
        return false;
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority) {
        int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
        if (i >= 0 && i < this.levelHeightAccessor.getSectionsCount()) {
            IChunkAccess.getOrCreateOffsetList(this.toBeTicked, i).add(ProtoChunk.packOffsetCoordinates(pos));
        }
    }

    @Override
    public boolean willTickThisTick(BlockPosition pos, T object) {
        return false;
    }

    @Override
    public int size() {
        return Stream.of(this.toBeTicked).filter(Objects::nonNull).mapToInt(List::size).sum();
    }
}
