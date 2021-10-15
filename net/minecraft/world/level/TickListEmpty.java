package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;

public class TickListEmpty<T> implements TickList<T> {
    private static final TickListEmpty<Object> INSTANCE = new TickListEmpty<>();

    public static <T> TickListEmpty<T> empty() {
        return INSTANCE;
    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T object) {
        return false;
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay) {
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority) {
    }

    @Override
    public boolean willTickThisTick(BlockPosition pos, T object) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }
}
