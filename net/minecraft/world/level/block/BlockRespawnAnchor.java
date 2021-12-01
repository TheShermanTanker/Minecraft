package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.DismountUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.ICollisionAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class BlockRespawnAnchor extends Block {
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 4;
    public static final BlockStateInteger CHARGE = BlockProperties.RESPAWN_ANCHOR_CHARGES;
    private static final ImmutableList<BaseBlockPosition> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(new BaseBlockPosition(0, 0, -1), new BaseBlockPosition(-1, 0, 0), new BaseBlockPosition(0, 0, 1), new BaseBlockPosition(1, 0, 0), new BaseBlockPosition(-1, 0, -1), new BaseBlockPosition(1, 0, -1), new BaseBlockPosition(-1, 0, 1), new BaseBlockPosition(1, 0, 1));
    private static final ImmutableList<BaseBlockPosition> RESPAWN_OFFSETS = (new Builder<BaseBlockPosition>()).addAll(RESPAWN_HORIZONTAL_OFFSETS).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(BaseBlockPosition::down).iterator()).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(BaseBlockPosition::up).iterator()).add(new BaseBlockPosition(0, 1, 0)).build();

    public BlockRespawnAnchor(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(CHARGE, Integer.valueOf(0)));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (hand == EnumHand.MAIN_HAND && !isRespawnFuel(itemStack) && isRespawnFuel(player.getItemInHand(EnumHand.OFF_HAND))) {
            return EnumInteractionResult.PASS;
        } else if (isRespawnFuel(itemStack) && canBeCharged(state)) {
            charge(world, pos, state);
            if (!player.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else if (state.get(CHARGE) == 0) {
            return EnumInteractionResult.PASS;
        } else if (!canSetSpawn(world)) {
            if (!world.isClientSide) {
                this.explode(state, world, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            if (!world.isClientSide) {
                EntityPlayer serverPlayer = (EntityPlayer)player;
                if (serverPlayer.getSpawnDimension() != world.getDimensionKey() || !pos.equals(serverPlayer.getSpawn())) {
                    serverPlayer.setRespawnPosition(world.getDimensionKey(), pos, 0.0F, false, true);
                    world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.RESPAWN_ANCHOR_SET_SPAWN, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                    return EnumInteractionResult.SUCCESS;
                }
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    private static boolean isRespawnFuel(ItemStack stack) {
        return stack.is(Items.GLOWSTONE);
    }

    private static boolean canBeCharged(IBlockData state) {
        return state.get(CHARGE) < 4;
    }

    private static boolean isWaterThatWouldFlow(BlockPosition pos, World world) {
        Fluid fluidState = world.getFluid(pos);
        if (!fluidState.is(TagsFluid.WATER)) {
            return false;
        } else if (fluidState.isSource()) {
            return true;
        } else {
            float f = (float)fluidState.getAmount();
            if (f < 2.0F) {
                return false;
            } else {
                Fluid fluidState2 = world.getFluid(pos.below());
                return !fluidState2.is(TagsFluid.WATER);
            }
        }
    }

    private void explode(IBlockData state, World world, BlockPosition explodedPos) {
        world.removeBlock(explodedPos, false);
        boolean bl = EnumDirection.EnumDirectionLimit.HORIZONTAL.stream().map(explodedPos::relative).anyMatch((pos) -> {
            return isWaterThatWouldFlow(pos, world);
        });
        final boolean bl2 = bl || world.getFluid(explodedPos.above()).is(TagsFluid.WATER);
        ExplosionDamageCalculator explosionDamageCalculator = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState) {
                return pos.equals(explodedPos) && bl2 ? Optional.of(Blocks.WATER.getDurability()) : super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState);
            }
        };
        world.createExplosion((Entity)null, DamageSource.badRespawnPointExplosion(), explosionDamageCalculator, (double)explodedPos.getX() + 0.5D, (double)explodedPos.getY() + 0.5D, (double)explodedPos.getZ() + 0.5D, 5.0F, true, Explosion.Effect.DESTROY);
    }

    public static boolean canSetSpawn(World world) {
        return world.getDimensionManager().isRespawnAnchorWorks();
    }

    public static void charge(World world, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, state.set(CHARGE, Integer.valueOf(state.get(CHARGE) + 1)), 3);
        world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.RESPAWN_ANCHOR_CHARGE, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(CHARGE) != 0) {
            if (random.nextInt(100) == 0) {
                world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.RESPAWN_ANCHOR_AMBIENT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            double d = (double)pos.getX() + 0.5D + (0.5D - random.nextDouble());
            double e = (double)pos.getY() + 1.0D;
            double f = (double)pos.getZ() + 0.5D + (0.5D - random.nextDouble());
            double g = (double)random.nextFloat() * 0.04D;
            world.addParticle(Particles.REVERSE_PORTAL, d, e, f, 0.0D, g, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(CHARGE);
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    public static int getScaledChargeLevel(IBlockData state, int maxLevel) {
        return MathHelper.floor((float)(state.get(CHARGE) - 0) / 4.0F * (float)maxLevel);
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return getScaledChargeLevel(state, 15);
    }

    public static Optional<Vec3D> findStandUpPosition(EntityTypes<?> entity, ICollisionAccess world, BlockPosition pos) {
        Optional<Vec3D> optional = findStandUpPosition(entity, world, pos, true);
        return optional.isPresent() ? optional : findStandUpPosition(entity, world, pos, false);
    }

    private static Optional<Vec3D> findStandUpPosition(EntityTypes<?> entity, ICollisionAccess world, BlockPosition pos, boolean ignoreInvalidPos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(BaseBlockPosition vec3i : RESPAWN_OFFSETS) {
            mutableBlockPos.set(pos).move(vec3i);
            Vec3D vec3 = DismountUtil.findSafeDismountLocation(entity, world, mutableBlockPos, ignoreInvalidPos);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
