package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public class BlockObserver extends BlockDirectional {
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;

    public BlockObserver(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.SOUTH).set(POWERED, Boolean.valueOf(false)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, POWERED);
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
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(POWERED)) {
            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 2);
        } else {
            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(true)), 2);
            world.getBlockTicks().scheduleTick(pos, this, 2);
        }

        this.updateNeighborsInFront(world, pos, state);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(FACING) == direction && !state.get(POWERED)) {
            this.startSignal(world, pos);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    private void startSignal(GeneratorAccess world, BlockPosition pos) {
        if (!world.isClientSide() && !world.getBlockTickList().hasScheduledTick(pos, this)) {
            world.getBlockTickList().scheduleTick(pos, this, 2);
        }

    }

    protected void updateNeighborsInFront(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        world.neighborChanged(blockPos, this, pos);
        world.updateNeighborsAtExceptFromFacing(blockPos, this, direction);
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) && state.get(FACING) == direction ? 15 : 0;
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!state.is(oldState.getBlock())) {
            if (!world.isClientSide() && state.get(POWERED) && !world.getBlockTickList().hasScheduledTick(pos, this)) {
                IBlockData blockState = state.set(POWERED, Boolean.valueOf(false));
                world.setTypeAndData(pos, blockState, 18);
                this.updateNeighborsInFront(world, pos, blockState);
            }

        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide && state.get(POWERED) && world.getBlockTickList().hasScheduledTick(pos, this)) {
                this.updateNeighborsInFront(world, pos, state.set(POWERED, Boolean.valueOf(false)));
            }

        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getNearestLookingDirection().opposite().opposite());
    }
}
