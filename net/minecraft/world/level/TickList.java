package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;

public interface TickList<T> {
    boolean hasScheduledTick(BlockPosition pos, T object);

    default void scheduleTick(BlockPosition pos, T object, int delay) {
        this.scheduleTick(pos, object, delay, TickListPriority.NORMAL);
    }

    void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority);

    boolean willTickThisTick(BlockPosition pos, T object);

    int size();
}
