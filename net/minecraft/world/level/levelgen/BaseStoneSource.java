package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

@FunctionalInterface
public interface BaseStoneSource {
    default IBlockData getBaseBlock(BlockPosition pos) {
        return this.getBaseBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    IBlockData getBaseBlock(int x, int y, int z);
}
