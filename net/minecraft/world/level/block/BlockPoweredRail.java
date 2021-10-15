package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockPoweredRail extends BlockMinecartTrackAbstract {
    public static final BlockStateEnum<BlockPropertyTrackPosition> SHAPE = BlockProperties.RAIL_SHAPE_STRAIGHT;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;

    protected BlockPoweredRail(BlockBase.Info settings) {
        super(true, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH).set(POWERED, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    protected boolean findPoweredRailSignal(World world, BlockPosition pos, IBlockData state, boolean bl, int distance) {
        if (distance >= 8) {
            return false;
        } else {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            boolean bl2 = true;
            BlockPropertyTrackPosition railShape = state.get(SHAPE);
            switch(railShape) {
            case NORTH_SOUTH:
                if (bl) {
                    ++k;
                } else {
                    --k;
                }
                break;
            case EAST_WEST:
                if (bl) {
                    --i;
                } else {
                    ++i;
                }
                break;
            case ASCENDING_EAST:
                if (bl) {
                    --i;
                } else {
                    ++i;
                    ++j;
                    bl2 = false;
                }

                railShape = BlockPropertyTrackPosition.EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (bl) {
                    --i;
                    ++j;
                    bl2 = false;
                } else {
                    ++i;
                }

                railShape = BlockPropertyTrackPosition.EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (bl) {
                    ++k;
                } else {
                    --k;
                    ++j;
                    bl2 = false;
                }

                railShape = BlockPropertyTrackPosition.NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (bl) {
                    ++k;
                    ++j;
                    bl2 = false;
                } else {
                    --k;
                }

                railShape = BlockPropertyTrackPosition.NORTH_SOUTH;
            }

            if (this.isSameRailWithPower(world, new BlockPosition(i, j, k), bl, distance, railShape)) {
                return true;
            } else {
                return bl2 && this.isSameRailWithPower(world, new BlockPosition(i, j - 1, k), bl, distance, railShape);
            }
        }
    }

    protected boolean isSameRailWithPower(World world, BlockPosition pos, boolean bl, int distance, BlockPropertyTrackPosition shape) {
        IBlockData blockState = world.getType(pos);
        if (!blockState.is(this)) {
            return false;
        } else {
            BlockPropertyTrackPosition railShape = blockState.get(SHAPE);
            if (shape != BlockPropertyTrackPosition.EAST_WEST || railShape != BlockPropertyTrackPosition.NORTH_SOUTH && railShape != BlockPropertyTrackPosition.ASCENDING_NORTH && railShape != BlockPropertyTrackPosition.ASCENDING_SOUTH) {
                if (shape != BlockPropertyTrackPosition.NORTH_SOUTH || railShape != BlockPropertyTrackPosition.EAST_WEST && railShape != BlockPropertyTrackPosition.ASCENDING_EAST && railShape != BlockPropertyTrackPosition.ASCENDING_WEST) {
                    if (blockState.get(POWERED)) {
                        return world.isBlockIndirectlyPowered(pos) ? true : this.findPoweredRailSignal(world, pos, blockState, bl, distance + 1);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    protected void updateState(IBlockData state, World world, BlockPosition pos, Block neighbor) {
        boolean bl = state.get(POWERED);
        boolean bl2 = world.isBlockIndirectlyPowered(pos) || this.findPoweredRailSignal(world, pos, state, true, 0) || this.findPoweredRailSignal(world, pos, state, false, 0);
        if (bl2 != bl) {
            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl2)), 3);
            world.applyPhysics(pos.below(), this);
            if (state.get(SHAPE).isAscending()) {
                world.applyPhysics(pos.above(), this);
            }
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
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
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
            }
        case CLOCKWISE_90:
            switch((BlockPropertyTrackPosition)state.get(SHAPE)) {
            case NORTH_SOUTH:
                return state.set(SHAPE, BlockPropertyTrackPosition.EAST_WEST);
            case EAST_WEST:
                return state.set(SHAPE, BlockPropertyTrackPosition.NORTH_SOUTH);
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
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }
}
