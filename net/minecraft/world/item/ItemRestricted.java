package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemRestricted extends ItemBlock {
    public ItemRestricted(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Nullable
    @Override
    protected IBlockData getPlacementState(BlockActionContext context) {
        EntityHuman player = context.getEntity();
        return player != null && !player.isCreativeAndOp() ? null : super.getPlacementState(context);
    }
}
