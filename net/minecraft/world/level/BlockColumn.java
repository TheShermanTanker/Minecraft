package net.minecraft.world.level;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public final class BlockColumn implements net.minecraft.world.level.chunk.BlockColumn {
    private final int minY;
    private final IBlockData[] column;

    public BlockColumn(int startY, IBlockData[] states) {
        this.minY = startY;
        this.column = states;
    }

    @Override
    public IBlockData getBlock(int y) {
        int i = y - this.minY;
        return i >= 0 && i < this.column.length ? this.column[i] : Blocks.AIR.getBlockData();
    }

    @Override
    public void setBlock(int y, IBlockData state) {
        int i = y - this.minY;
        if (i >= 0 && i < this.column.length) {
            this.column[i] = state;
        } else {
            throw new IllegalArgumentException("Outside of column height: " + y);
        }
    }
}
