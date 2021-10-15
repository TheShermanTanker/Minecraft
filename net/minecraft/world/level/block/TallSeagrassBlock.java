package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class TallSeagrassBlock extends BlockTallPlant implements IFluidContainer {
    public static final BlockStateEnum<BlockPropertyDoubleBlockHalf> HALF = BlockTallPlant.HALF;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public TallSeagrassBlock(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.isFaceSturdy(world, pos, EnumDirection.UP) && !floor.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Blocks.SEAGRASS);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = super.getPlacedState(ctx);
        if (blockState != null) {
            Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition().above());
            if (fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8) {
                return blockState;
            }
        }

        return null;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        if (state.get(HALF) == BlockPropertyDoubleBlockHalf.UPPER) {
            IBlockData blockState = world.getType(pos.below());
            return blockState.is(this) && blockState.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER;
        } else {
            Fluid fluidState = world.getFluid(pos);
            return super.canPlace(state, world, pos) && fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8;
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return FluidTypes.WATER.getSource(false);
    }

    @Override
    public boolean canPlace(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) {
        return false;
    }

    @Override
    public boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        return false;
    }
}
