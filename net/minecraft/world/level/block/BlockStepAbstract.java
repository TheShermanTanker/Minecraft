package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertySlabType;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockStepAbstract extends Block implements IBlockWaterlogged {
    public static final BlockStateEnum<BlockPropertySlabType> TYPE = BlockProperties.SLAB_TYPE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape BOTTOM_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    protected static final VoxelShape TOP_AABB = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public BlockStepAbstract(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(TYPE, BlockPropertySlabType.BOTTOM).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return state.get(TYPE) != BlockPropertySlabType.DOUBLE;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(TYPE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        BlockPropertySlabType slabType = state.get(TYPE);
        switch(slabType) {
        case DOUBLE:
            return VoxelShapes.block();
        case TOP:
            return TOP_AABB;
        default:
            return BOTTOM_AABB;
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPosition blockPos = ctx.getClickPosition();
        IBlockData blockState = ctx.getWorld().getType(blockPos);
        if (blockState.is(this)) {
            return blockState.set(TYPE, BlockPropertySlabType.DOUBLE).set(WATERLOGGED, Boolean.valueOf(false));
        } else {
            Fluid fluidState = ctx.getWorld().getFluid(blockPos);
            IBlockData blockState2 = this.getBlockData().set(TYPE, BlockPropertySlabType.BOTTOM).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
            EnumDirection direction = ctx.getClickedFace();
            return direction != EnumDirection.DOWN && (direction == EnumDirection.UP || !(ctx.getPos().y - (double)blockPos.getY() > 0.5D)) ? blockState2 : blockState2.set(TYPE, BlockPropertySlabType.TOP);
        }
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        ItemStack itemStack = context.getItemStack();
        BlockPropertySlabType slabType = state.get(TYPE);
        if (slabType != BlockPropertySlabType.DOUBLE && itemStack.is(this.getItem())) {
            if (context.replacingClickedOnBlock()) {
                boolean bl = context.getPos().y - (double)context.getClickPosition().getY() > 0.5D;
                EnumDirection direction = context.getClickedFace();
                if (slabType == BlockPropertySlabType.BOTTOM) {
                    return direction == EnumDirection.UP || bl && direction.getAxis().isHorizontal();
                } else {
                    return direction == EnumDirection.DOWN || !bl && direction.getAxis().isHorizontal();
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        return state.get(TYPE) != BlockPropertySlabType.DOUBLE ? IBlockWaterlogged.super.place(world, pos, state, fluidState) : false;
    }

    @Override
    public boolean canPlace(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) {
        return state.get(TYPE) != BlockPropertySlabType.DOUBLE ? IBlockWaterlogged.super.canPlace(world, pos, state, fluid) : false;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return false;
        case WATER:
            return world.getFluid(pos).is(TagsFluid.WATER);
        case AIR:
            return false;
        default:
            return false;
        }
    }
}
