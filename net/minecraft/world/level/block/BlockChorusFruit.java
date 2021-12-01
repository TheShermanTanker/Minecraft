package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;

public class BlockChorusFruit extends BlockSprawling {
    protected BlockChorusFruit(BlockBase.Info settings) {
        super(0.3125F, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)).set(UP, Boolean.valueOf(false)).set(DOWN, Boolean.valueOf(false)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getStateForPlacement(ctx.getWorld(), ctx.getClickPosition());
    }

    public IBlockData getStateForPlacement(IBlockAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        IBlockData blockState2 = world.getType(pos.above());
        IBlockData blockState3 = world.getType(pos.north());
        IBlockData blockState4 = world.getType(pos.east());
        IBlockData blockState5 = world.getType(pos.south());
        IBlockData blockState6 = world.getType(pos.west());
        return this.getBlockData().set(DOWN, Boolean.valueOf(blockState.is(this) || blockState.is(Blocks.CHORUS_FLOWER) || blockState.is(Blocks.END_STONE))).set(UP, Boolean.valueOf(blockState2.is(this) || blockState2.is(Blocks.CHORUS_FLOWER))).set(NORTH, Boolean.valueOf(blockState3.is(this) || blockState3.is(Blocks.CHORUS_FLOWER))).set(EAST, Boolean.valueOf(blockState4.is(this) || blockState4.is(Blocks.CHORUS_FLOWER))).set(SOUTH, Boolean.valueOf(blockState5.is(this) || blockState5.is(Blocks.CHORUS_FLOWER))).set(WEST, Boolean.valueOf(blockState6.is(this) || blockState6.is(Blocks.CHORUS_FLOWER)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!state.canPlace(world, pos)) {
            world.scheduleTick(pos, this, 1);
            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            boolean bl = neighborState.is(this) || neighborState.is(Blocks.CHORUS_FLOWER) || direction == EnumDirection.DOWN && neighborState.is(Blocks.END_STONE);
            return state.set(PROPERTY_BY_DIRECTION.get(direction), Boolean.valueOf(bl));
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        boolean bl = !world.getType(pos.above()).isAir() && !blockState.isAir();

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            IBlockData blockState2 = world.getType(blockPos);
            if (blockState2.is(this)) {
                if (bl) {
                    return false;
                }

                IBlockData blockState3 = world.getType(blockPos.below());
                if (blockState3.is(this) || blockState3.is(Blocks.END_STONE)) {
                    return true;
                }
            }
        }

        return blockState.is(this) || blockState.is(Blocks.END_STONE);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
