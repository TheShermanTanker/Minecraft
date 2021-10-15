package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockMinecartTrack extends BlockMinecartTrackAbstract {
    public static final BlockStateEnum<BlockPropertyTrackPosition> SHAPE = BlockProperties.RAIL_SHAPE;

    protected BlockMinecartTrack(BlockBase.Info settings) {
        super(false, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    protected void updateState(IBlockData state, World world, BlockPosition pos, Block neighbor) {
        if (neighbor.getBlockData().isPowerSource() && (new MinecartTrackLogic(world, pos, state)).countPotentialConnections() == 3) {
            this.updateDir(world, pos, state, false);
        }

    }

    @Override
    public IBlockState<BlockPropertyTrackPosition> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            }
        case COUNTERCLOCKWISE_90:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
            }
        case CLOCKWISE_90:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
            }
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        BlockPropertyTrackPosition railShape = state.get(SHAPE);
        switch(mirror) {
        case LEFT_RIGHT:
            switch(railShape) {
            case ASCENDING_NORTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            default:
                return super.mirror(state, mirror);
            }
        case FRONT_BACK:
            switch(railShape) {
            case ASCENDING_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.ASCENDING_EAST);
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
            default:
                break;
            case SOUTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_WEST);
            case SOUTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.SOUTH_EAST);
            case NORTH_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_EAST);
            case NORTH_EAST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_WEST);
            }
        }

        return super.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }
}
