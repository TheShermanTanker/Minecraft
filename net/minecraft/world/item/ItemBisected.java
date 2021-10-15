package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemBisected extends ItemBlock {
    public ItemBisected(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Override
    protected boolean placeBlock(BlockActionContext context, IBlockData state) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition().above();
        IBlockData blockState = level.isWaterAt(blockPos) ? Blocks.WATER.getBlockData() : Blocks.AIR.getBlockData();
        level.setTypeAndData(blockPos, blockState, 27);
        return super.placeBlock(context, state);
    }
}
