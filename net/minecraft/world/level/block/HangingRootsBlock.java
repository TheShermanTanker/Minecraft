package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class HangingRootsBlock extends Block implements IBlockWaterlogged {
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 10.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    protected HangingRootsBlock(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = super.getPlacedState(ctx);
        if (blockState != null) {
            Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
            return blockState.set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
        } else {
            return null;
        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = world.getType(blockPos);
        return blockState.isFaceSturdy(world, blockPos, EnumDirection.DOWN);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.UP && !this.canPlace(state, world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            if (state.get(WATERLOGGED)) {
                world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }
}
