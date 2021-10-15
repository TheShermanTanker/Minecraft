package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockGrowingStem extends BlockGrowingAbstract implements IBlockFragilePlantElement {
    protected BlockGrowingStem(BlockBase.Info settings, EnumDirection growthDirection, VoxelShape outlineShape, boolean tickWater) {
        super(settings, growthDirection, outlineShape, tickWater);
    }

    protected IBlockData updateHeadAfterConvertedFromBody(IBlockData from, IBlockData to) {
        return to;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == this.growthDirection.opposite() && !state.canPlace(world, pos)) {
            world.getBlockTickList().scheduleTick(pos, this, 1);
        }

        BlockGrowingTop growingPlantHeadBlock = this.getHeadBlock();
        if (direction == this.growthDirection && !neighborState.is(this) && !neighborState.is(growingPlantHeadBlock)) {
            return this.updateHeadAfterConvertedFromBody(state, growingPlantHeadBlock.getStateForPlacement(world));
        } else {
            if (this.scheduleFluidTicks) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(this.getHeadBlock());
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        Optional<BlockPosition> optional = this.getHeadPos(world, pos, state.getBlock());
        return optional.isPresent() && this.getHeadBlock().canGrowInto(world.getType(optional.get().relative(this.growthDirection)));
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        Optional<BlockPosition> optional = this.getHeadPos(world, pos, state.getBlock());
        if (optional.isPresent()) {
            IBlockData blockState = world.getType(optional.get());
            ((BlockGrowingTop)blockState.getBlock()).performBonemeal(world, random, optional.get(), blockState);
        }

    }

    private Optional<BlockPosition> getHeadPos(IBlockAccess world, BlockPosition pos, Block block) {
        return BlockUtil.getTopConnectedBlock(world, pos, block, this.growthDirection, this.getHeadBlock());
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        boolean bl = super.canBeReplaced(state, context);
        return bl && context.getItemStack().is(this.getHeadBlock().getItem()) ? false : bl;
    }

    @Override
    protected Block getBodyBlock() {
        return this;
    }
}
