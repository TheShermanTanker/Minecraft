package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class TintedGlassBlock extends BlockGlassAbstract {
    public TintedGlassBlock(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return false;
    }

    @Override
    public int getLightBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return world.getMaxLightLevel();
    }
}
