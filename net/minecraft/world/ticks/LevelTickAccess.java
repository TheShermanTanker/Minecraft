package net.minecraft.world.ticks;

import net.minecraft.core.BlockPosition;

public interface LevelTickAccess<T> extends TickAccess<T> {
    boolean willTickThisTick(BlockPosition pos, T type);
}
