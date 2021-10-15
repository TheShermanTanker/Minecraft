package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSeagrass extends BlockPlant implements IBlockFragilePlantElement, IFluidContainer {
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    protected BlockSeagrass(BlockBase.Info settings) {
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

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return fluidState.is(TagsFluid.WATER) && fluidState.getAmount() == 8 ? super.getPlacedState(ctx) : null;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        IBlockData blockState = super.updateState(state, direction, neighborState, world, pos, neighborPos);
        if (!blockState.isAir()) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return blockState;
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return FluidTypes.WATER.getSource(false);
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        IBlockData blockState = Blocks.TALL_SEAGRASS.getBlockData();
        IBlockData blockState2 = blockState.set(TallSeagrassBlock.HALF, BlockPropertyDoubleBlockHalf.UPPER);
        BlockPosition blockPos = pos.above();
        if (world.getType(blockPos).is(Blocks.WATER)) {
            world.setTypeAndData(pos, blockState, 2);
            world.setTypeAndData(blockPos, blockState2, 2);
        }

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
