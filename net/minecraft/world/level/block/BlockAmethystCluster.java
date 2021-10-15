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
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockAmethystCluster extends BlockAmethyst implements IBlockWaterlogged {
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateDirection FACING = BlockProperties.FACING;
    protected final VoxelShape northAabb;
    protected final VoxelShape southAabb;
    protected final VoxelShape eastAabb;
    protected final VoxelShape westAabb;
    protected final VoxelShape upAabb;
    protected final VoxelShape downAabb;

    public BlockAmethystCluster(int height, int xzOffset, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)).set(FACING, EnumDirection.UP));
        this.upAabb = Block.box((double)xzOffset, 0.0D, (double)xzOffset, (double)(16 - xzOffset), (double)height, (double)(16 - xzOffset));
        this.downAabb = Block.box((double)xzOffset, (double)(16 - height), (double)xzOffset, (double)(16 - xzOffset), 16.0D, (double)(16 - xzOffset));
        this.northAabb = Block.box((double)xzOffset, (double)xzOffset, (double)(16 - height), (double)(16 - xzOffset), (double)(16 - xzOffset), 16.0D);
        this.southAabb = Block.box((double)xzOffset, (double)xzOffset, 0.0D, (double)(16 - xzOffset), (double)(16 - xzOffset), (double)height);
        this.eastAabb = Block.box(0.0D, (double)xzOffset, (double)xzOffset, (double)height, (double)(16 - xzOffset), (double)(16 - xzOffset));
        this.westAabb = Block.box((double)(16 - height), (double)xzOffset, (double)xzOffset, 16.0D, (double)(16 - xzOffset), (double)(16 - xzOffset));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        EnumDirection direction = state.get(FACING);
        switch(direction) {
        case NORTH:
            return this.northAabb;
        case SOUTH:
            return this.southAabb;
        case EAST:
            return this.eastAabb;
        case WEST:
            return this.westAabb;
        case DOWN:
            return this.downAabb;
        case UP:
        default:
            return this.upAabb;
        }
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        return world.getType(blockPos).isFaceSturdy(world, blockPos, direction);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction == state.get(FACING).opposite() && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        GeneratorAccess levelAccessor = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(levelAccessor.getFluid(blockPos).getType() == FluidTypes.WATER)).set(FACING, ctx.getClickedFace());
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
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }
}
