package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;

public class DispenseBehaviorShears extends DispenseBehaviorMaybe {
    @Override
    protected ItemStack execute(ISourceBlock pointer, ItemStack stack) {
        World level = pointer.getWorld();
        if (!level.isClientSide()) {
            BlockPosition blockPos = pointer.getBlockPosition().relative(pointer.getBlockData().get(BlockDispenser.FACING));
            this.setSuccess(tryShearBeehive((WorldServer)level, blockPos) || tryShearLivingEntity((WorldServer)level, blockPos));
            if (this.isSuccess() && stack.isDamaged(1, level.getRandom(), (EntityPlayer)null)) {
                stack.setCount(0);
            }
        }

        return stack;
    }

    private static boolean tryShearBeehive(WorldServer world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (blockState.is(TagsBlock.BEEHIVES)) {
            int i = blockState.get(BlockBeehive.HONEY_LEVEL);
            if (i >= 5) {
                world.playSound((EntityHuman)null, pos, SoundEffects.BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
                BlockBeehive.dropHoneycomb(world, pos);
                ((BlockBeehive)blockState.getBlock()).releaseBeesAndResetHoneyLevel(world, blockState, pos, (EntityHuman)null, TileEntityBeehive.ReleaseStatus.BEE_RELEASED);
                world.gameEvent((Entity)null, GameEvent.SHEAR, pos);
                return true;
            }
        }

        return false;
    }

    private static boolean tryShearLivingEntity(WorldServer world, BlockPosition pos) {
        for(EntityLiving livingEntity : world.getEntitiesOfClass(EntityLiving.class, new AxisAlignedBB(pos), IEntitySelector.NO_SPECTATORS)) {
            if (livingEntity instanceof IShearable) {
                IShearable shearable = (IShearable)livingEntity;
                if (shearable.canShear()) {
                    shearable.shear(SoundCategory.BLOCKS);
                    world.gameEvent((Entity)null, GameEvent.SHEAR, pos);
                    return true;
                }
            }
        }

        return false;
    }
}
