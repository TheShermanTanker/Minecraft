package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityChestTrapped extends TileEntityChest {
    public TileEntityChestTrapped(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.TRAPPED_CHEST, pos, state);
    }

    @Override
    protected void signalOpenCount(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount) {
        super.signalOpenCount(world, pos, state, oldViewerCount, newViewerCount);
        if (oldViewerCount != newViewerCount) {
            Block block = state.getBlock();
            world.applyPhysics(pos, block);
            world.applyPhysics(pos.below(), block);
        }

    }
}
