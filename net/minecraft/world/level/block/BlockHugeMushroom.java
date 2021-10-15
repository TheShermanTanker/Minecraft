package net.minecraft.world.level.block;

import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public class BlockHugeMushroom extends Block {
    public static final BlockStateBoolean NORTH = BlockSprawling.NORTH;
    public static final BlockStateBoolean EAST = BlockSprawling.EAST;
    public static final BlockStateBoolean SOUTH = BlockSprawling.SOUTH;
    public static final BlockStateBoolean WEST = BlockSprawling.WEST;
    public static final BlockStateBoolean UP = BlockSprawling.UP;
    public static final BlockStateBoolean DOWN = BlockSprawling.DOWN;
    private static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockSprawling.PROPERTY_BY_DIRECTION;

    public BlockHugeMushroom(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(NORTH, Boolean.valueOf(true)).set(EAST, Boolean.valueOf(true)).set(SOUTH, Boolean.valueOf(true)).set(WEST, Boolean.valueOf(true)).set(UP, Boolean.valueOf(true)).set(DOWN, Boolean.valueOf(true)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockAccess blockGetter = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        return this.getBlockData().set(DOWN, Boolean.valueOf(!blockGetter.getType(blockPos.below()).is(this))).set(UP, Boolean.valueOf(!blockGetter.getType(blockPos.above()).is(this))).set(NORTH, Boolean.valueOf(!blockGetter.getType(blockPos.north()).is(this))).set(EAST, Boolean.valueOf(!blockGetter.getType(blockPos.east()).is(this))).set(SOUTH, Boolean.valueOf(!blockGetter.getType(blockPos.south()).is(this))).set(WEST, Boolean.valueOf(!blockGetter.getType(blockPos.west()).is(this)));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return neighborState.is(this) ? state.set(PROPERTY_BY_DIRECTION.get(direction), Boolean.valueOf(false)) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.NORTH)), state.get(NORTH)).set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.SOUTH)), state.get(SOUTH)).set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.EAST)), state.get(EAST)).set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.WEST)), state.get(WEST)).set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.UP)), state.get(UP)).set(PROPERTY_BY_DIRECTION.get(rotation.rotate(EnumDirection.DOWN)), state.get(DOWN));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.NORTH)), state.get(NORTH)).set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.SOUTH)), state.get(SOUTH)).set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.EAST)), state.get(EAST)).set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.WEST)), state.get(WEST)).set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.UP)), state.get(UP)).set(PROPERTY_BY_DIRECTION.get(mirror.mirror(EnumDirection.DOWN)), state.get(DOWN));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }
}
