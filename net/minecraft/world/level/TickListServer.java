package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class TickListServer<T> implements TickList<T> {
    public static final int MAX_TICK_BLOCKS_PER_TICK = 65536;
    protected final Predicate<T> ignore;
    private final Function<T, MinecraftKey> toId;
    private final Set<NextTickListEntry<T>> tickNextTickSet = Sets.newHashSet();
    private final Set<NextTickListEntry<T>> tickNextTickList = Sets.newTreeSet(NextTickListEntry.createTimeComparator());
    private final WorldServer level;
    private final Queue<NextTickListEntry<T>> currentlyTicking = Queues.newArrayDeque();
    private final List<NextTickListEntry<T>> alreadyTicked = Lists.newArrayList();
    private final Consumer<NextTickListEntry<T>> ticker;

    public TickListServer(WorldServer world, Predicate<T> invalidObjPredicate, Function<T, MinecraftKey> idToName, Consumer<NextTickListEntry<T>> tickConsumer) {
        this.ignore = invalidObjPredicate;
        this.toId = idToName;
        this.level = world;
        this.ticker = tickConsumer;
    }

    public void tick() {
        int i = this.tickNextTickList.size();
        if (i != this.tickNextTickSet.size()) {
            throw new IllegalStateException("TickNextTick list out of synch");
        } else {
            if (i > 65536) {
                i = 65536;
            }

            Iterator<NextTickListEntry<T>> iterator = this.tickNextTickList.iterator();
            this.level.getMethodProfiler().enter("cleaning");

            while(i > 0 && iterator.hasNext()) {
                NextTickListEntry<T> tickNextTickData = iterator.next();
                if (tickNextTickData.triggerTick > this.level.getTime()) {
                    break;
                }

                if (this.level.isPositionTickingWithEntitiesLoaded(tickNextTickData.pos)) {
                    iterator.remove();
                    this.tickNextTickSet.remove(tickNextTickData);
                    this.currentlyTicking.add(tickNextTickData);
                    --i;
                }
            }

            this.level.getMethodProfiler().exitEnter("ticking");

            NextTickListEntry<T> tickNextTickData2;
            while((tickNextTickData2 = this.currentlyTicking.poll()) != null) {
                if (this.level.isPositionTickingWithEntitiesLoaded(tickNextTickData2.pos)) {
                    try {
                        this.alreadyTicked.add(tickNextTickData2);
                        this.ticker.accept(tickNextTickData2);
                    } catch (Throwable var7) {
                        CrashReport crashReport = CrashReport.forThrowable(var7, "Exception while ticking");
                        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block being ticked");
                        CrashReportSystemDetails.populateBlockDetails(crashReportCategory, this.level, tickNextTickData2.pos, (IBlockData)null);
                        throw new ReportedException(crashReport);
                    }
                } else {
                    this.scheduleTick(tickNextTickData2.pos, tickNextTickData2.getType(), 0);
                }
            }

            this.level.getMethodProfiler().exit();
            this.alreadyTicked.clear();
            this.currentlyTicking.clear();
        }
    }

    @Override
    public boolean willTickThisTick(BlockPosition pos, T object) {
        return this.currentlyTicking.contains(new NextTickListEntry(pos, object));
    }

    public List<NextTickListEntry<T>> fetchTicksInChunk(ChunkCoordIntPair pos, boolean updateState, boolean getStaleTicks) {
        int i = pos.getMinBlockX() - 2;
        int j = i + 16 + 2;
        int k = pos.getMinBlockZ() - 2;
        int l = k + 16 + 2;
        return this.fetchTicksInArea(new StructureBoundingBox(i, this.level.getMinBuildHeight(), k, j, this.level.getMaxBuildHeight(), l), updateState, getStaleTicks);
    }

    public List<NextTickListEntry<T>> fetchTicksInArea(StructureBoundingBox bounds, boolean updateState, boolean getStaleTicks) {
        List<NextTickListEntry<T>> list = this.fetchTicksInArea((List<NextTickListEntry<T>>)null, this.tickNextTickList, bounds, updateState);
        if (updateState && list != null) {
            this.tickNextTickSet.removeAll(list);
        }

        list = this.fetchTicksInArea(list, this.currentlyTicking, bounds, updateState);
        if (!getStaleTicks) {
            list = this.fetchTicksInArea(list, this.alreadyTicked, bounds, updateState);
        }

        return list == null ? Collections.emptyList() : list;
    }

    @Nullable
    private List<NextTickListEntry<T>> fetchTicksInArea(@Nullable List<NextTickListEntry<T>> dst, Collection<NextTickListEntry<T>> src, StructureBoundingBox bounds, boolean move) {
        Iterator<NextTickListEntry<T>> iterator = src.iterator();

        while(iterator.hasNext()) {
            NextTickListEntry<T> tickNextTickData = iterator.next();
            BlockPosition blockPos = tickNextTickData.pos;
            if (blockPos.getX() >= bounds.minX() && blockPos.getX() < bounds.maxX() && blockPos.getZ() >= bounds.minZ() && blockPos.getZ() < bounds.maxZ()) {
                if (move) {
                    iterator.remove();
                }

                if (dst == null) {
                    dst = Lists.newArrayList();
                }

                dst.add(tickNextTickData);
            }
        }

        return dst;
    }

    public void copy(StructureBoundingBox box, BlockPosition offset) {
        for(NextTickListEntry<T> tickNextTickData : this.fetchTicksInArea(box, false, false)) {
            if (box.isInside(tickNextTickData.pos)) {
                BlockPosition blockPos = tickNextTickData.pos.offset(offset);
                T object = tickNextTickData.getType();
                this.addTickData(new NextTickListEntry<>(blockPos, object, tickNextTickData.triggerTick, tickNextTickData.priority));
            }
        }

    }

    public NBTTagList save(ChunkCoordIntPair chunkPos) {
        List<NextTickListEntry<T>> list = this.fetchTicksInChunk(chunkPos, false, true);
        return saveTickList(this.toId, list, this.level.getTime());
    }

    public static <T> NBTTagList saveTickList(Function<T, MinecraftKey> identifierProvider, Iterable<NextTickListEntry<T>> scheduledTicks, long time) {
        NBTTagList listTag = new NBTTagList();

        for(NextTickListEntry<T> tickNextTickData : scheduledTicks) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setString("i", identifierProvider.apply(tickNextTickData.getType()).toString());
            compoundTag.setInt("x", tickNextTickData.pos.getX());
            compoundTag.setInt("y", tickNextTickData.pos.getY());
            compoundTag.setInt("z", tickNextTickData.pos.getZ());
            compoundTag.setInt("t", (int)(tickNextTickData.triggerTick - time));
            compoundTag.setInt("p", tickNextTickData.priority.getValue());
            listTag.add(compoundTag);
        }

        return listTag;
    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T object) {
        return this.tickNextTickSet.contains(new NextTickListEntry(pos, object));
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority) {
        if (!this.ignore.test(object)) {
            this.addTickData(new NextTickListEntry<>(pos, object, (long)delay + this.level.getTime(), priority));
        }

    }

    private void addTickData(NextTickListEntry<T> tick) {
        if (!this.tickNextTickSet.contains(tick)) {
            this.tickNextTickSet.add(tick);
            this.tickNextTickList.add(tick);
        }

    }

    @Override
    public int size() {
        return this.tickNextTickSet.size();
    }
}
