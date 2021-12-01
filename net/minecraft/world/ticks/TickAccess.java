package net.minecraft.world.ticks;

import net.minecraft.core.BlockPosition;

public interface TickAccess<T> {
    void schedule(ScheduledTick<T> orderedTick);

    boolean hasScheduledTick(BlockPosition pos, T type);

    int count();
}
