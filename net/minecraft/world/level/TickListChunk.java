package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;

public class TickListChunk<T> implements TickList<T> {
    private final List<TickListChunk.ScheduledTick<T>> ticks;
    private final Function<T, MinecraftKey> toId;

    public TickListChunk(Function<T, MinecraftKey> identifierProvider, List<NextTickListEntry<T>> scheduledTicks, long startTime) {
        this(identifierProvider, scheduledTicks.stream().map((tickNextTickData) -> {
            return new TickListChunk.ScheduledTick(tickNextTickData.getType(), tickNextTickData.pos, (int)(tickNextTickData.triggerTick - startTime), tickNextTickData.priority);
        }).collect(Collectors.toList()));
    }

    private TickListChunk(Function<T, MinecraftKey> identifierProvider, List<TickListChunk.ScheduledTick<T>> scheduledTicks) {
        this.ticks = scheduledTicks;
        this.toId = identifierProvider;
    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T object) {
        return false;
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority) {
        this.ticks.add(new TickListChunk.ScheduledTick<>(object, pos, delay, priority));
    }

    @Override
    public boolean willTickThisTick(BlockPosition pos, T object) {
        return false;
    }

    public NBTTagList save() {
        NBTTagList listTag = new NBTTagList();

        for(TickListChunk.ScheduledTick<T> scheduledTick : this.ticks) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setString("i", this.toId.apply(scheduledTick.type).toString());
            compoundTag.setInt("x", scheduledTick.pos.getX());
            compoundTag.setInt("y", scheduledTick.pos.getY());
            compoundTag.setInt("z", scheduledTick.pos.getZ());
            compoundTag.setInt("t", scheduledTick.delay);
            compoundTag.setInt("p", scheduledTick.priority.getValue());
            listTag.add(compoundTag);
        }

        return listTag;
    }

    public static <T> TickListChunk<T> create(NBTTagList ticks, Function<T, MinecraftKey> function, Function<MinecraftKey, T> function2) {
        List<TickListChunk.ScheduledTick<T>> list = Lists.newArrayList();

        for(int i = 0; i < ticks.size(); ++i) {
            NBTTagCompound compoundTag = ticks.getCompound(i);
            T object = function2.apply(new MinecraftKey(compoundTag.getString("i")));
            if (object != null) {
                BlockPosition blockPos = new BlockPosition(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
                list.add(new TickListChunk.ScheduledTick<>(object, blockPos, compoundTag.getInt("t"), TickListPriority.byValue(compoundTag.getInt("p"))));
            }
        }

        return new TickListChunk<>(function, list);
    }

    public void copyOut(TickList<T> scheduler) {
        this.ticks.forEach((tick) -> {
            scheduler.scheduleTick(tick.pos, tick.type, tick.delay, tick.priority);
        });
    }

    @Override
    public int size() {
        return this.ticks.size();
    }

    static class ScheduledTick<T> {
        final T type;
        public final BlockPosition pos;
        public final int delay;
        public final TickListPriority priority;

        ScheduledTick(T object, BlockPosition pos, int delay, TickListPriority priority) {
            this.type = object;
            this.pos = pos;
            this.delay = delay;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return this.type + ": " + this.pos + ", " + this.delay + ", " + this.priority;
        }
    }
}
