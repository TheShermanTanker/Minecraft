package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockTNT extends Block {
    public static final BlockStateBoolean UNSTABLE = BlockProperties.UNSTABLE;

    public BlockTNT(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(UNSTABLE, Boolean.valueOf(false)));
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (world.isBlockIndirectlyPowered(pos)) {
                explode(world, pos);
                world.removeBlock(pos, false);
            }

        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (world.isBlockIndirectlyPowered(pos)) {
            explode(world, pos);
            world.removeBlock(pos, false);
        }

    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide() && !player.isCreative() && state.get(UNSTABLE)) {
            explode(world, pos);
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void wasExploded(World world, BlockPosition pos, Explosion explosion) {
        if (!world.isClientSide) {
            EntityTNTPrimed primedTnt = new EntityTNTPrimed(world, (double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, explosion.getSource());
            int i = primedTnt.getFuseTicks();
            primedTnt.setFuseTicks((short)(world.random.nextInt(i / 4) + i / 8));
            world.addEntity(primedTnt);
        }
    }

    public static void explode(World world, BlockPosition pos) {
        explode(world, pos, (EntityLiving)null);
    }

    private static void explode(World world, BlockPosition pos, @Nullable EntityLiving igniter) {
        if (!world.isClientSide) {
            EntityTNTPrimed primedTnt = new EntityTNTPrimed(world, (double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, igniter);
            world.addEntity(primedTnt);
            world.playSound((EntityHuman)null, primedTnt.locX(), primedTnt.locY(), primedTnt.locZ(), SoundEffects.TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        }
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.FLINT_AND_STEEL) && !itemStack.is(Items.FIRE_CHARGE)) {
            return super.interact(state, world, pos, player, hand, hit);
        } else {
            explode(world, pos, player);
            world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 11);
            Item item = itemStack.getItem();
            if (!player.isCreative()) {
                if (itemStack.is(Items.FLINT_AND_STEEL)) {
                    itemStack.damage(1, player, (playerx) -> {
                        playerx.broadcastItemBreak(hand);
                    });
                } else {
                    itemStack.subtract(1);
                }
            }

            player.awardStat(StatisticList.ITEM_USED.get(item));
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        if (!world.isClientSide) {
            BlockPosition blockPos = hit.getBlockPosition();
            Entity entity = projectile.getShooter();
            if (projectile.isBurning() && projectile.mayInteract(world, blockPos)) {
                explode(world, blockPos, entity instanceof EntityLiving ? (EntityLiving)entity : null);
                world.removeBlock(blockPos, false);
            }
        }

    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(UNSTABLE);
    }
}
