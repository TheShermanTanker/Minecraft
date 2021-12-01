package net.minecraft.world.item;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockGrowingTop;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemShears extends Item {
    public ItemShears(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if (!world.isClientSide && !state.is(TagsBlock.FIRE)) {
            stack.damage(1, miner, (e) -> {
                e.broadcastItemBreak(EnumItemSlot.MAINHAND);
            });
        }

        return !state.is(TagsBlock.LEAVES) && !state.is(Blocks.COBWEB) && !state.is(Blocks.GRASS) && !state.is(Blocks.FERN) && !state.is(Blocks.DEAD_BUSH) && !state.is(Blocks.HANGING_ROOTS) && !state.is(Blocks.VINE) && !state.is(Blocks.TRIPWIRE) && !state.is(TagsBlock.WOOL) ? super.mineBlock(stack, world, state, pos, miner) : true;
    }

    @Override
    public boolean canDestroySpecialBlock(IBlockData state) {
        return state.is(Blocks.COBWEB) || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.TRIPWIRE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockData state) {
        if (!state.is(Blocks.COBWEB) && !state.is(TagsBlock.LEAVES)) {
            if (state.is(TagsBlock.WOOL)) {
                return 5.0F;
            } else {
                return !state.is(Blocks.VINE) && !state.is(Blocks.GLOW_LICHEN) ? super.getDestroySpeed(stack, state) : 2.0F;
            }
        } else {
            return 15.0F;
        }
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof BlockGrowingTop) {
            BlockGrowingTop growingPlantHeadBlock = (BlockGrowingTop)block;
            if (!growingPlantHeadBlock.isMaxAge(blockState)) {
                EntityHuman player = context.getEntity();
                ItemStack itemStack = context.getItemStack();
                if (player instanceof EntityPlayer) {
                    CriterionTriggers.ITEM_USED_ON_BLOCK.trigger((EntityPlayer)player, blockPos, itemStack);
                }

                level.playSound(player, blockPos, SoundEffects.GROWING_PLANT_CROP, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                level.setTypeUpdate(blockPos, growingPlantHeadBlock.getMaxAgeState(blockState));
                if (player != null) {
                    itemStack.damage(1, player, (playerx) -> {
                        playerx.broadcastItemBreak(context.getHand());
                    });
                }

                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.useOn(context);
    }
}
