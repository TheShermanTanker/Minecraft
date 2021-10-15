package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockBarrier extends Block {
    protected BlockBarrier(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return true;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return 1.0F;
    }
}
