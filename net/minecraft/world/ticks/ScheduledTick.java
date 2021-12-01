package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;

public record ScheduledTick<T>(T type, BlockPosition pos, long triggerTick, TickPriority priority, long subTickOrder) {
    public static final Comparator<ScheduledTick<?>> DRAIN_ORDER = (first, second) -> {
        int i = Long.compare(first.triggerTick, second.triggerTick);
        if (i != 0) {
            return i;
        } else {
            i = first.priority.compareTo(second.priority);
            return i != 0 ? i : Long.compare(first.subTickOrder, second.subTickOrder);
        }
    };
    public static final Comparator<ScheduledTick<?>> INTRA_TICK_DRAIN_ORDER = (first, second) -> {
        int i = first.priority.compareTo(second.priority);
        return i != 0 ? i : Long.compare(first.subTickOrder, second.subTickOrder);
    };
    public static final Strategy<ScheduledTick<?>> UNIQUE_TICK_HASH = new Strategy<ScheduledTick<?>>() {
        @Override
        public int hashCode(ScheduledTick<?> scheduledTick) {
            return 31 * scheduledTick.pos().hashCode() + scheduledTick.type().hashCode();
        }

        @Override
        public boolean equals(@Nullable ScheduledTick<?> scheduledTick, @Nullable ScheduledTick<?> scheduledTick2) {
            if (scheduledTick == scheduledTick2) {
                return true;
            } else if (scheduledTick != null && scheduledTick2 != null) {
                return scheduledTick.type() == scheduledTick2.type() && scheduledTick.pos().equals(scheduledTick2.pos());
            } else {
                return false;
            }
        }
    };

    public ScheduledTick(T type, BlockPosition pos, long triggerTick, long subTickOrder) {
        this(type, pos, triggerTick, TickPriority.NORMAL, subTickOrder);
    }

    public ScheduledTick(T object, BlockPosition blockPos, long l, TickPriority tickPriority, long m) {
        blockPos = blockPos.immutableCopy();
        this.type = object;
        this.pos = blockPos;
        this.triggerTick = l;
        this.priority = tickPriority;
        this.subTickOrder = m;
    }

    public static <T> ScheduledTick<T> probe(T type, BlockPosition pos) {
        return new ScheduledTick<>(type, pos, 0L, TickPriority.NORMAL, 0L);
    }

    public T type() {
        return this.type;
    }

    public BlockPosition pos() {
        return this.pos;
    }

    public long triggerTick() {
        return this.triggerTick;
    }

    public TickPriority priority() {
        return this.priority;
    }

    public long subTickOrder() {
        return this.subTickOrder;
    }
}
