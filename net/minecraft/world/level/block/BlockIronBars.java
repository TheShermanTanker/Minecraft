package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockIronBars extends BlockTall {
    protected BlockIronBars(BlockBase.Info settings) {
        super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        BlockPosition blockPos2 = blockPos.north();
        BlockPosition blockPos3 = blockPos.south();
        BlockPosition blockPos4 = blockPos.west();
        BlockPosition blockPos5 = blockPos.east();
        IBlockData blockState = blockGetter.getType(blockPos2);
        IBlockData blockState2 = blockGetter.getType(blockPos3);
        IBlockData blockState3 = blockGetter.getType(blockPos4);
        IBlockData blockState4 = blockGetter.getType(blockPos5);
        return this.getBlockData().set(NORTH, Boolean.valueOf(this.attachsTo(blockState, blockState.isFaceSturdy(blockGetter, blockPos2, EnumDirection.SOUTH)))).set(SOUTH, Boolean.valueOf(this.attachsTo(blockState2, blockState2.isFaceSturdy(blockGetter, blockPos3, EnumDirection.NORTH)))).set(WEST, Boolean.valueOf(this.attachsTo(blockState3, blockState3.isFaceSturdy(blockGetter, blockPos4, EnumDirection.EAST)))).set(EAST, Boolean.valueOf(this.attachsTo(blockState4, blockState4.isFaceSturdy(blockGetter, blockPos5, EnumDirection.WEST)))).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction.getAxis().isHorizontal() ? state.set(PROPERTY_BY_DIRECTION.get(direction), Boolean.valueOf(this.attachsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction.opposite())))) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getVisualShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean skipRendering(IBlockData state, IBlockData stateFrom, EnumDirection direction) {
        if (stateFrom.is(this)) {
            if (!direction.getAxis().isHorizontal()) {
                return true;
            }

            if (state.get(PROPERTY_BY_DIRECTION.get(direction)) && stateFrom.get(PROPERTY_BY_DIRECTION.get(direction.opposite()))) {
                return true;
            }
        }

        return super.skipRendering(state, stateFrom, direction);
    }

    public final boolean attachsTo(IBlockData state, boolean sideSolidFullSquare) {
        return !isExceptionForConnection(state) && sideSolidFullSquare || state.getBlock() instanceof BlockIronBars || state.is(TagsBlock.WALLS);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
