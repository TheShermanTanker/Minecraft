package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemFireball extends Item {
    public ItemFireball(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        boolean bl = false;
        if (!BlockCampfire.canLight(blockState) && !CandleBlock.canLight(blockState) && !CandleCakeBlock.canLight(blockState)) {
            blockPos = blockPos.relative(context.getClickedFace());
            if (BlockFireAbstract.canBePlacedAt(level, blockPos, context.getHorizontalDirection())) {
                this.playSound(level, blockPos);
                level.setTypeUpdate(blockPos, BlockFireAbstract.getState(level, blockPos));
                level.gameEvent(context.getEntity(), GameEvent.BLOCK_PLACE, blockPos);
                bl = true;
            }
        } else {
            this.playSound(level, blockPos);
            level.setTypeUpdate(blockPos, blockState.set(BlockProperties.LIT, Boolean.valueOf(true)));
            level.gameEvent(context.getEntity(), GameEvent.BLOCK_PLACE, blockPos);
            bl = true;
        }

        if (bl) {
            context.getItemStack().subtract(1);
            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return EnumInteractionResult.FAIL;
        }
    }

    private void playSound(World world, BlockPosition pos) {
        Random random = world.getRandom();
        world.playSound((EntityHuman)null, pos, SoundEffects.FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
