package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
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

public class BlockCoralBase extends Block implements IBlockWaterlogged {
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final VoxelShape AABB = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);

    protected BlockCoralBase(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(WATERLOGGED, Boolean.valueOf(true)));
    }

    protected void tryScheduleDieTick(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        if (!scanForWater(state, world, pos)) {
            world.scheduleTick(pos, this, 60 + world.getRandom().nextInt(40));
        }

    }

    protected static boolean scanForWater(IBlockData state, IBlockAccess world, BlockPosition pos) {
        if (state.get(WATERLOGGED)) {
            return true;
        } else {
            for(EnumDirection direction : EnumDirection.values()) {
                if (world.getFluid(pos.relative(direction)).is(TagsFluid.WATER)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return AABB;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction == EnumDirection.DOWN && !this.canPlace(state, world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return world.getType(blockPos).isFaceSturdy(world, blockPos, EnumDirection.UP);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }
}
