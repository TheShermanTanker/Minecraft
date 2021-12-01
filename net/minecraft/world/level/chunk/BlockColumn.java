package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.IBlockData;

public interface BlockColumn {
    IBlockData getBlock(int y);

    void setBlock(int y, IBlockData state);
}
