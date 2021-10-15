package net.minecraft.server.level;

import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickListPriority;

public class TickListWorldGen<T> implements TickList<T> {
    private final Function<BlockPosition, TickList<T>> index;

    public TickListWorldGen(Function<BlockPosition, TickList<T>> mapper) {
        this.index = mapper;
    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T object) {
        return this.index.apply(pos).hasScheduledTick(pos, object);
    }

    @Override
    public void scheduleTick(BlockPosition pos, T object, int delay, TickListPriority priority) {
        this.index.apply(pos).scheduleTick(pos, object, delay, priority);
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
