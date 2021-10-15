package net.minecraft.world.item;

import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemBed extends ItemBlock {
    public ItemBed(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Override
    protected boolean placeBlock(BlockActionContext context, IBlockData state) {
        return context.getWorld().setTypeAndData(context.getClickPosition(), state, 26);
    }
}
