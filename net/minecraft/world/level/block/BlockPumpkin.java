package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockPumpkin extends BlockStemmed {
    protected BlockPumpkin(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.SHEARS)) {
            if (!world.isClientSide) {
                EnumDirection direction = hit.getDirection();
                EnumDirection direction2 = direction.getAxis() == EnumDirection.EnumAxis.Y ? player.getDirection().opposite() : direction;
                world.playSound((EntityHuman)null, pos, SoundEffects.PUMPKIN_CARVE, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                world.setTypeAndData(pos, Blocks.CARVED_PUMPKIN.getBlockData().set(BlockPumpkinCarved.FACING, direction2), 11);
                EntityItem itemEntity = new EntityItem(world, (double)pos.getX() + 0.5D + (double)direction2.getAdjacentX() * 0.65D, (double)pos.getY() + 0.1D, (double)pos.getZ() + 0.5D + (double)direction2.getAdjacentZ() * 0.65D, new ItemStack(Items.PUMPKIN_SEEDS, 4));
                itemEntity.setMot(0.05D * (double)direction2.getAdjacentX() + world.random.nextDouble() * 0.02D, 0.05D, 0.05D * (double)direction2.getAdjacentZ() + world.random.nextDouble() * 0.02D);
                world.addEntity(itemEntity);
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
                world.gameEvent(player, GameEvent.SHEAR, pos);
                player.awardStat(StatisticList.ITEM_USED.get(Items.SHEARS));
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return super.interact(state, world, pos, player, hand, hit);
        }
    }

    @Override
    public BlockStem getStem() {
        return (BlockStem)Blocks.PUMPKIN_STEM;
    }

    @Override
    public BlockStemAttached getAttachedStem() {
        return (BlockStemAttached)Blocks.ATTACHED_PUMPKIN_STEM;
    }
}
