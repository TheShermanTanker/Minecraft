package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class BlockLightningRod extends BlockRod implements IBlockWaterlogged {
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private static final int ACTIVATION_TICKS = 8;
    public static final int RANGE = 128;
    private static final int SPARK_CYCLE = 200;

    public BlockLightningRod(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.UP).set(WATERLOGGED, Boolean.valueOf(false)).set(POWERED, Boolean.valueOf(false)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        boolean bl = fluidState.getType() == FluidTypes.WATER;
        return this.getBlockData().set(FACING, ctx.getClickedFace()).set(WATERLOGGED, Boolean.valueOf(bl));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) && state.get(FACING) == direction ? 15 : 0;
    }

    public void onLightningStrike(IBlockData state, World world, BlockPosition pos) {
        world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(true)), 3);
        this.updateNeighbours(state, world, pos);
        world.getBlockTickList().scheduleTick(pos, this, 8);
        world.triggerEffect(3002, pos, state.get(FACING).getAxis().ordinal());
    }

    private void updateNeighbours(IBlockData state, World world, BlockPosition pos) {
        world.applyPhysics(pos.relative(state.get(FACING).opposite()), this);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 3);
        this.updateNeighbours(state, world, pos);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (world.isThundering() && (long)world.random.nextInt(200) <= world.getTime() % 200L && pos.getY() == world.getHeight(HeightMap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1) {
            ParticleUtils.spawnParticlesAlongAxis(state.get(FACING).getAxis(), world, pos, 0.125D, Particles.ELECTRIC_SPARK, UniformInt.of(1, 2));
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (state.get(POWERED)) {
                this.updateNeighbours(state, world, pos);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!state.is(oldState.getBlock())) {
            if (state.get(POWERED) && !world.getBlockTickList().hasScheduledTick(pos, this)) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 18);
            }

        }
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        if (world.isThundering() && projectile instanceof EntityThrownTrident && ((EntityThrownTrident)projectile).isChanneling()) {
            BlockPosition blockPos = hit.getBlockPosition();
            if (world.canSeeSky(blockPos)) {
                EntityLightning lightningBolt = EntityTypes.LIGHTNING_BOLT.create(world);
                lightningBolt.moveTo(Vec3D.atBottomCenterOf(blockPos.above()));
                Entity entity = projectile.getShooter();
                lightningBolt.setCause(entity instanceof EntityPlayer ? (EntityPlayer)entity : null);
                world.addEntity(lightningBolt);
                world.playSound((EntityHuman)null, blockPos, SoundEffects.TRIDENT_THUNDER, SoundCategory.WEATHER, 5.0F, 1.0F);
            }
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, POWERED, WATERLOGGED);
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }
}
