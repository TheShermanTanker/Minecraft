package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockMinecartTrackAbstract extends Block implements IBlockWaterlogged {
    protected static final VoxelShape FLAT_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    protected static final VoxelShape HALF_BLOCK_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private final boolean isStraight;

    public static boolean isRail(World world, BlockPosition pos) {
        return isRail(world.getType(pos));
    }

    public static boolean isRail(IBlockData state) {
        return state.is(TagsBlock.RAILS) && state.getBlock() instanceof BlockMinecartTrackAbstract;
    }

    protected BlockMinecartTrackAbstract(boolean allowCurves, BlockBase.Info settings) {
        super(settings);
        this.isStraight = allowCurves;
    }

    public boolean isStraight() {
        return this.isStraight;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        BlockPropertyTrackPosition railShape = state.is(this) ? state.get(this.getShapeProperty()) : null;
        return railShape != null && railShape.isAscending() ? HALF_BLOCK_AABB : FLAT_AABB;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return canSupportRigidBlock(world, pos.below());
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateState(state, world, pos, notify);
        }
    }

    protected IBlockData updateState(IBlockData state, World world, BlockPosition pos, boolean notify) {
        state = this.updateDir(world, pos, state, true);
        if (this.isStraight) {
            state.doPhysics(world, pos, this, pos, notify);
        }

        return state;
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide && world.getType(pos).is(this)) {
            BlockPropertyTrackPosition railShape = state.get(this.getShapeProperty());
            if (shouldBeRemoved(pos, world, railShape)) {
                dropResources(state, world, pos);
                world.removeBlock(pos, notify);
            } else {
                this.updateState(state, world, pos, block);
            }

        }
    }

    private static boolean shouldBeRemoved(BlockPosition pos, World world, BlockPropertyTrackPosition shape) {
        if (!canSupportRigidBlock(world, pos.below())) {
            return true;
        } else {
            switch(shape) {
            case ASCENDING_EAST:
                return !canSupportRigidBlock(world, pos.east());
            case ASCENDING_WEST:
                return !canSupportRigidBlock(world, pos.west());
            case ASCENDING_NORTH:
                return !canSupportRigidBlock(world, pos.north());
            case ASCENDING_SOUTH:
                return !canSupportRigidBlock(world, pos.south());
            default:
                return false;
            }
        }
    }

    protected void updateState(IBlockData state, World world, BlockPosition pos, Block neighbor) {
    }

    protected IBlockData updateDir(World world, BlockPosition pos, IBlockData state, boolean forceUpdate) {
        if (world.isClientSide) {
            return state;
        } else {
            BlockPropertyTrackPosition railShape = state.get(this.getShapeProperty());
            return (new MinecartTrackLogic(world, pos, state)).place(world.isBlockIndirectlyPowered(pos), forceUpdate, railShape).getState();
        }
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.NORMAL;
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved) {
            super.remove(state, world, pos, newState, moved);
            if (state.get(this.getShapeProperty()).isAscending()) {
                world.applyPhysics(pos.above(), this);
            }

            if (this.isStraight) {
                world.applyPhysics(pos, this);
                world.applyPhysics(pos.below(), this);
            }

        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        boolean bl = fluidState.getType() == FluidTypes.WATER;
        IBlockData blockState = super.getBlockData();
        EnumDirection direction = ctx.getHorizontalDirection();
        boolean bl2 = direction == EnumDirection.EAST || direction == EnumDirection.WEST;
        return blockState.set(this.getShapeProperty(), bl2 ? BlockPropertyTrackPosition.EAST_WEST : BlockPropertyTrackPosition.NORTH_SOUTH).set(WATERLOGGED, Boolean.valueOf(bl));
    }

    public abstract IBlockState<BlockPropertyTrackPosition> getShapeProperty();

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }
}
