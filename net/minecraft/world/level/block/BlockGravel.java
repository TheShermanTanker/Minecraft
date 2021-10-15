package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockGravel extends BlockFalling {
    public BlockGravel(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public int getDustColor(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return -8356741;
    }
}
