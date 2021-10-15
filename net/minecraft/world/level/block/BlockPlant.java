package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;

public class BlockPlant extends Block {
    protected BlockPlant(BlockBase.Info settings) {
        super(settings);
    }

    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(TagsBlock.DIRT) || floor.is(Blocks.FARMLAND);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return this.mayPlaceOn(world.getType(blockPos), world, blockPos);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.getFluid().isEmpty();
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return type == PathMode.AIR && !this.hasCollision ? true : super.isPathfindable(state, world, pos, type);
    }
}
