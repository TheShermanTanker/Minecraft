package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockRepeater extends BlockDiodeAbstract {
    public static final BlockStateBoolean LOCKED = BlockProperties.LOCKED;
    public static final BlockStateInteger DELAY = BlockProperties.DELAY;

    protected BlockRepeater(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(DELAY, Integer.valueOf(1)).set(LOCKED, Boolean.valueOf(false)).set(POWERED, Boolean.valueOf(false)));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (!player.getAbilities().mayBuild) {
            return EnumInteractionResult.PASS;
        } else {
            world.setTypeAndData(pos, state.cycle(DELAY), 3);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    @Override
    protected int getDelay(IBlockData state) {
        return state.get(DELAY) * 2;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = super.getPlacedState(ctx);
        return blockState.set(LOCKED, Boolean.valueOf(this.isLocked(ctx.getWorld(), ctx.getClickPosition(), blockState)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return !world.isClientSide() && direction.getAxis() != state.get(FACING).getAxis() ? state.set(LOCKED, Boolean.valueOf(this.isLocked(world, pos, state))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean isLocked(IWorldReader world, BlockPosition pos, IBlockData state) {
        return this.getAlternateSignal(world, pos, state) > 0;
    }

    @Override
    protected boolean isAlternateInput(IBlockData state) {
        return isDiode(state);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(POWERED)) {
            EnumDirection direction = state.get(FACING);
            double d = (double)pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            double e = (double)pos.getY() + 0.4D + (random.nextDouble() - 0.5D) * 0.2D;
            double f = (double)pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            float g = -5.0F;
            if (random.nextBoolean()) {
                g = (float)(state.get(DELAY) * 2 - 1);
            }

            g = g / 16.0F;
            double h = (double)(g * (float)direction.getAdjacentX());
            double i = (double)(g * (float)direction.getAdjacentZ());
            world.addParticle(ParticleParamRedstone.REDSTONE, d + h, e, f + i, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, DELAY, LOCKED, POWERED);
    }
}
