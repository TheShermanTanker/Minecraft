package net.minecraft.world.item;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemSpade extends ItemTool {
    protected static final Map<Block, IBlockData> FLATTENABLES = Maps.newHashMap((new Builder()).put(Blocks.GRASS_BLOCK, Blocks.DIRT_PATH.getBlockData()).put(Blocks.DIRT, Blocks.DIRT_PATH.getBlockData()).put(Blocks.PODZOL, Blocks.DIRT_PATH.getBlockData()).put(Blocks.COARSE_DIRT, Blocks.DIRT_PATH.getBlockData()).put(Blocks.MYCELIUM, Blocks.DIRT_PATH.getBlockData()).put(Blocks.ROOTED_DIRT, Blocks.DIRT_PATH.getBlockData()).build());

    public ItemSpade(ToolMaterial material, float attackDamage, float attackSpeed, Item.Info settings) {
        super(attackDamage, attackSpeed, material, TagsBlock.MINEABLE_WITH_SHOVEL, settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (context.getClickedFace() == EnumDirection.DOWN) {
            return EnumInteractionResult.PASS;
        } else {
            EntityHuman player = context.getEntity();
            IBlockData blockState2 = FLATTENABLES.get(blockState.getBlock());
            IBlockData blockState3 = null;
            if (blockState2 != null && level.getType(blockPos.above()).isAir()) {
                level.playSound(player, blockPos, SoundEffects.SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                blockState3 = blockState2;
            } else if (blockState.getBlock() instanceof BlockCampfire && blockState.get(BlockCampfire.LIT)) {
                if (!level.isClientSide()) {
                    level.triggerEffect((EntityHuman)null, 1009, blockPos, 0);
                }

                BlockCampfire.dowse(context.getEntity(), level, blockPos, blockState);
                blockState3 = blockState.set(BlockCampfire.LIT, Boolean.valueOf(false));
            }

            if (blockState3 != null) {
                if (!level.isClientSide) {
                    level.setTypeAndData(blockPos, blockState3, 11);
                    if (player != null) {
                        context.getItemStack().damage(1, player, (p) -> {
                            p.broadcastItemBreak(context.getHand());
                        });
                    }
                }

                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }
}
