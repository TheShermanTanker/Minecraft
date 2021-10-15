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
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockLadder extends Block implements IBlockWaterlogged {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);

    protected BlockLadder(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection)state.get(FACING)) {
        case NORTH:
            return NORTH_AABB;
        case SOUTH:
            return SOUTH_AABB;
        case WEST:
            return WEST_AABB;
        case EAST:
        default:
            return EAST_AABB;
        }
    }

    private boolean canAttachTo(IBlockAccess world, BlockPosition pos, EnumDirection side) {
        IBlockData blockState = world.getType(pos);
        return blockState.isFaceSturdy(world, pos, side);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        return this.canAttachTo(world, pos.relative(direction.opposite()), direction);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction.opposite() == state.get(FACING) && !state.canPlace(world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            if (state.get(WATERLOGGED)) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        if (!ctx.replacingClickedOnBlock()) {
            IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().relative(ctx.getClickedFace().opposite()));
            if (blockState.is(this) && blockState.get(FACING) == ctx.getClickedFace()) {
                return null;
            }
        }

        IBlockData blockState2 = this.getBlockData();
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());

        for(EnumDirection direction : ctx.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                blockState2 = blockState2.set(FACING, direction.opposite());
                if (blockState2.canPlace(levelReader, blockPos)) {
                    return blockState2.set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
                }
            }
        }

        return null;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }
}
