package net.minecraft.world.ticks;

import java.util.function.Function;
import net.minecraft.core.BlockPosition;

public class WorldGenTickAccess<T> implements LevelTickAccess<T> {
    private final Function<BlockPosition, TickContainerAccess<T>> containerGetter;

    public WorldGenTickAccess(Function<BlockPosition, TickContainerAccess<T>> mapper) {
        this.containerGetter = mapper;
    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T type) {
        return this.containerGetter.apply(pos).hasScheduledTick(pos, type);
    }

    @Override
    public void schedule(ScheduledTick<T> orderedTick) {
        this.containerGetter.apply(orderedTick.pos()).schedule(orderedTick);
    }

    @Override
    public boolean willTickThisTick(BlockPosition pos, T type) {
        return false;
    }

    @Override
    public int count() {
        return 0;
    }
}
