package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemSign extends ItemBlockWallable {
    public ItemSign(Item.Info settings, Block standingBlock, Block wallBlock) {
        super(standingBlock, wallBlock, settings);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPosition pos, World world, @Nullable EntityHuman player, ItemStack stack, IBlockData state) {
        boolean bl = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
        if (!world.isClientSide && !bl && player != null) {
            player.openSign((TileEntitySign)world.getTileEntity(pos));
        }

        return bl;
    }
}
