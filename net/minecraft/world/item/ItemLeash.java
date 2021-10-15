package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.decoration.EntityLeash;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;

public class ItemLeash extends Item {
    public ItemLeash(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (blockState.is(TagsBlock.FENCES)) {
            EntityHuman player = context.getEntity();
            if (!level.isClientSide && player != null) {
                bindPlayerMobs(player, level, blockPos);
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    public static EnumInteractionResult bindPlayerMobs(EntityHuman player, World world, BlockPosition pos) {
        EntityLeash leashFenceKnotEntity = null;
        boolean bl = false;
        double d = 7.0D;
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();

        for(EntityInsentient mob : world.getEntitiesOfClass(EntityInsentient.class, new AxisAlignedBB((double)i - 7.0D, (double)j - 7.0D, (double)k - 7.0D, (double)i + 7.0D, (double)j + 7.0D, (double)k + 7.0D))) {
            if (mob.getLeashHolder() == player) {
                if (leashFenceKnotEntity == null) {
                    leashFenceKnotEntity = EntityLeash.getOrCreateKnot(world, pos);
                    leashFenceKnotEntity.playPlaceSound();
                }

                mob.setLeashHolder(leashFenceKnotEntity, true);
                bl = true;
            }
        }

        return bl ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
    }
}
