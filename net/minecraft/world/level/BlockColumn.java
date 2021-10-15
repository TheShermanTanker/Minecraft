package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public final class BlockColumn {
    private final int minY;
    private final IBlockData[] column;

    public BlockColumn(int startY, IBlockData[] states) {
        this.minY = startY;
        this.column = states;
    }

    public IBlockData getBlockState(BlockPosition pos) {
        int i = pos.getY() - this.minY;
        return i >= 0 && i < this.column.length ? this.column[i] : Blocks.AIR.getBlockData();
    }
}
