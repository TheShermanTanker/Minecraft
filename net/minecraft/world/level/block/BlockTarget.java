package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class BlockTarget extends Block {
    private static final BlockStateInteger OUTPUT_POWER = BlockProperties.POWER;
    private static final int ACTIVATION_TICKS_ARROWS = 20;
    private static final int ACTIVATION_TICKS_OTHER = 8;

    public BlockTarget(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(OUTPUT_POWER, Integer.valueOf(0)));
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        int i = updateRedstoneOutput(world, state, hit, projectile);
        Entity entity = projectile.getShooter();
        if (entity instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)entity;
            serverPlayer.awardStat(StatisticList.TARGET_HIT);
            CriterionTriggers.TARGET_BLOCK_HIT.trigger(serverPlayer, projectile, hit.getPos(), i);
        }

    }

    private static int updateRedstoneOutput(GeneratorAccess world, IBlockData state, MovingObjectPositionBlock hitResult, Entity entity) {
        int i = getRedstoneStrength(hitResult, hitResult.getPos());
        int j = entity instanceof EntityArrow ? 20 : 8;
        if (!world.getBlockTickList().hasScheduledTick(hitResult.getBlockPosition(), state.getBlock())) {
            setOutputPower(world, state, i, hitResult.getBlockPosition(), j);
        }

        return i;
    }

    private static int getRedstoneStrength(MovingObjectPositionBlock hitResult, Vec3D pos) {
        EnumDirection direction = hitResult.getDirection();
        double d = Math.abs(MathHelper.frac(pos.x) - 0.5D);
        double e = Math.abs(MathHelper.frac(pos.y) - 0.5D);
        double f = Math.abs(MathHelper.frac(pos.z) - 0.5D);
        EnumDirection.EnumAxis axis = direction.getAxis();
        double g;
        if (axis == EnumDirection.EnumAxis.Y) {
            g = Math.max(d, f);
        } else if (axis == EnumDirection.EnumAxis.Z) {
            g = Math.max(d, e);
        } else {
            g = Math.max(e, f);
        }

        return Math.max(1, MathHelper.ceil(15.0D * MathHelper.clamp((0.5D - g) / 0.5D, 0.0D, 1.0D)));
    }

    private static void setOutputPower(GeneratorAccess world, IBlockData state, int power, BlockPosition pos, int delay) {
        world.setTypeAndData(pos, state.set(OUTPUT_POWER, Integer.valueOf(power)), 3);
        world.getBlockTickList().scheduleTick(pos, state.getBlock(), delay);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(OUTPUT_POWER) != 0) {
            world.setTypeAndData(pos, state.set(OUTPUT_POWER, Integer.valueOf(0)), 3);
        }

    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(OUTPUT_POWER);
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(OUTPUT_POWER);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!world.isClientSide() && !state.is(oldState.getBlock())) {
            if (state.get(OUTPUT_POWER) > 0 && !world.getBlockTickList().hasScheduledTick(pos, this)) {
                world.setTypeAndData(pos, state.set(OUTPUT_POWER, Integer.valueOf(0)), 18);
            }

        }
    }
}
