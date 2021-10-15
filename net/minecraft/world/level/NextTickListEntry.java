package net.minecraft.world.level;

import java.util.Comparator;
import net.minecraft.core.BlockPosition;

public class NextTickListEntry<T> {
    private static long counter;
    private final T type;
    public final BlockPosition pos;
    public final long triggerTick;
    public final TickListPriority priority;
    private final long c;

    public NextTickListEntry(BlockPosition pos, T t) {
        this(pos, t, 0L, TickListPriority.NORMAL);
    }

    public NextTickListEntry(BlockPosition pos, T t, long time, TickListPriority priority) {
        this.c = (long)(counter++);
        this.pos = pos.immutableCopy();
        this.type = t;
        this.triggerTick = time;
        this.priority = priority;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof NextTickListEntry)) {
            return false;
        } else {
            NextTickListEntry<?> tickNextTickData = (NextTickListEntry)object;
            return this.pos.equals(tickNextTickData.pos) && this.type == tickNextTickData.type;
        }
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }

    public static <T> Comparator<NextTickListEntry<T>> createTimeComparator() {
        return Comparator.comparingLong((tickNextTickData) -> {
            return tickNextTickData.triggerTick;
        }).thenComparing((tickNextTickData) -> {
            return tickNextTickData.priority;
        }).thenComparingLong((tickNextTickData) -> {
            return tickNextTickData.c;
        });
    }

    @Override
    public String toString() {
        return this.type + ": " + this.pos + ", " + this.triggerTick + ", " + this.priority + ", " + this.c;
    }

    public T getType() {
        return this.type;
    }
}
