package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockKelp extends BlockGrowingTop implements IFluidContainer {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    private static final double GROW_PER_TICK_PROBABILITY = 0.14D;

    protected BlockKelp(BlockBase.Info settings) {
        super(settings, EnumDirection.UP, SHAPE, true, 0.14D);
    }

    @Override
    protected boolean canGrowInto(IBlockData state) {
        return state.is(Blocks.WATER);
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.KELP_PLANT;
    }

    @Override
    protected boolean canAttachTo(IBlockData state) {
        return !state.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    public boolean canPlace(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) {
        return false;
    }

    @Override
    public boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        return false;
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(Random random) {
        return 1;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8 ? super.getPlacedState(ctx) : null;
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return FluidTypes.WATER.getSource(false);
    }
}
