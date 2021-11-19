package net.minecraft.world.item;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockCandle;
import net.minecraft.world.level.block.BlockCandleCake;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemFlintAndSteel extends Item {
    public ItemFlintAndSteel(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        EntityHuman player = context.getEntity();
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (!BlockCampfire.canLight(blockState) && !BlockCandle.canLight(blockState) && !BlockCandleCake.canLight(blockState)) {
            BlockPosition blockPos2 = blockPos.relative(context.getClickedFace());
            if (BlockFireAbstract.canBePlacedAt(level, blockPos2, context.getHorizontalDirection())) {
                level.playSound(player, blockPos2, SoundEffects.FLINTANDSTEEL_USE, EnumSoundCategory.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                IBlockData blockState2 = BlockFireAbstract.getState(level, blockPos2);
                level.setTypeAndData(blockPos2, blockState2, 11);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, blockPos);
                ItemStack itemStack = context.getItemStack();
                if (player instanceof EntityPlayer) {
                    CriterionTriggers.PLACED_BLOCK.trigger((EntityPlayer)player, blockPos2, itemStack);
                    itemStack.damage(1, player, (p) -> {
                        p.broadcastItemBreak(context.getHand());
                    });
                }

                return EnumInteractionResult.sidedSuccess(level.isClientSide());
            } else {
                return EnumInteractionResult.FAIL;
            }
        } else {
            level.playSound(player, blockPos, SoundEffects.FLINTANDSTEEL_USE, EnumSoundCategory.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setTypeAndData(blockPos, blockState.set(BlockProperties.LIT, Boolean.valueOf(true)), 11);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, blockPos);
            if (player != null) {
                context.getItemStack().damage(1, player, (p) -> {
                    p.broadcastItemBreak(context.getHand());
                });
            }

            return EnumInteractionResult.sidedSuccess(level.isClientSide());
        }
    }
}
