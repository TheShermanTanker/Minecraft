package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockCauldronLava extends BlockCauldronAbstract {
    public BlockCauldronLava(BlockBase.Info settings) {
        super(settings, CauldronInteraction.LAVA);
    }

    @Override
    protected double getContentHeight(IBlockData state) {
        return 0.9375D;
    }

    @Override
    public boolean isFull(IBlockData state) {
        return true;
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.burnFromLava();
        }

    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return 3;
    }
}
