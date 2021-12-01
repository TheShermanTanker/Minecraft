package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockScaffolding extends Block implements IBlockWaterlogged {
    private static final int TICK_DELAY = 1;
    private static final VoxelShape STABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE_BOTTOM = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    private static final VoxelShape BELOW_BLOCK = VoxelShapes.block().move(0.0D, -1.0D, 0.0D);
    public static final int STABILITY_MAX_DISTANCE = 7;
    public static final BlockStateInteger DISTANCE = BlockProperties.STABILITY_DISTANCE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateBoolean BOTTOM = BlockProperties.BOTTOM;

    protected BlockScaffolding(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(DISTANCE, Integer.valueOf(7)).set(WATERLOGGED, Boolean.valueOf(false)).set(BOTTOM, Boolean.valueOf(false)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(DISTANCE, WATERLOGGED, BOTTOM);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (!context.isHoldingItem(state.getBlock().getItem())) {
            return state.get(BOTTOM) ? UNSTABLE_SHAPE : STABLE_SHAPE;
        } else {
            return VoxelShapes.block();
        }
    }

    @Override
    public VoxelShape getInteractionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.block();
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return context.getItemStack().is(this.getItem());
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPosition blockPos = ctx.getClickPosition();
        World level = ctx.getWorld();
        int i = getDistance(level, blockPos);
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(level.getFluid(blockPos).getType() == FluidTypes.WATER)).set(DISTANCE, Integer.valueOf(i)).set(BOTTOM, Boolean.valueOf(this.isBottom(level, blockPos, i)));
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!world.isClientSide) {
            world.scheduleTick(pos, this, 1);
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        if (!world.isClientSide()) {
            world.scheduleTick(pos, this, 1);
        }

        return state;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        int i = getDistance(world, pos);
        IBlockData blockState = state.set(DISTANCE, Integer.valueOf(i)).set(BOTTOM, Boolean.valueOf(this.isBottom(world, pos, i)));
        if (blockState.get(DISTANCE) == 7) {
            if (state.get(DISTANCE) == 7) {
                world.addEntity(new EntityFallingBlock(world, (double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, blockState.set(WATERLOGGED, Boolean.valueOf(false))));
            } else {
                world.destroyBlock(pos, true);
            }
        } else if (state != blockState) {
            world.setTypeAndData(pos, blockState, 3);
        }

    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return getDistance(world, pos) < 7;
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (context.isAbove(VoxelShapes.block(), pos, true) && !context.isDescending()) {
            return STABLE_SHAPE;
        } else {
            return state.get(DISTANCE) != 0 && state.get(BOTTOM) && context.isAbove(BELOW_BLOCK, pos, true) ? UNSTABLE_SHAPE_BOTTOM : VoxelShapes.empty();
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    private boolean isBottom(IBlockAccess world, BlockPosition pos, int distance) {
        return distance > 0 && !world.getType(pos.below()).is(this);
    }

    public static int getDistance(IBlockAccess world, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable().move(EnumDirection.DOWN);
        IBlockData blockState = world.getType(mutableBlockPos);
        int i = 7;
        if (blockState.is(Blocks.SCAFFOLDING)) {
            i = blockState.get(DISTANCE);
        } else if (blockState.isFaceSturdy(world, mutableBlockPos, EnumDirection.UP)) {
            return 0;
        }

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            IBlockData blockState2 = world.getType(mutableBlockPos.setWithOffset(pos, direction));
            if (blockState2.is(Blocks.SCAFFOLDING)) {
                i = Math.min(i, blockState2.get(DISTANCE) + 1);
                if (i == 1) {
                    break;
                }
            }
        }

        return i;
    }

    static {
        VoxelShape voxelShape = Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        VoxelShape voxelShape2 = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D);
        VoxelShape voxelShape3 = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
        VoxelShape voxelShape4 = Block.box(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D);
        VoxelShape voxelShape5 = Block.box(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
        STABLE_SHAPE = VoxelShapes.or(voxelShape, voxelShape2, voxelShape3, voxelShape4, voxelShape5);
        VoxelShape voxelShape6 = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D);
        VoxelShape voxelShape7 = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
        VoxelShape voxelShape8 = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D);
        VoxelShape voxelShape9 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D);
        UNSTABLE_SHAPE = VoxelShapes.or(BlockScaffolding.UNSTABLE_SHAPE_BOTTOM, STABLE_SHAPE, voxelShape7, voxelShape6, voxelShape9, voxelShape8);
    }
}
