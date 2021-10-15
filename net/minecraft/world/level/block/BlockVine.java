package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
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
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockVine extends Block {
    public static final BlockStateBoolean UP = BlockSprawling.UP;
    public static final BlockStateBoolean NORTH = BlockSprawling.NORTH;
    public static final BlockStateBoolean EAST = BlockSprawling.EAST;
    public static final BlockStateBoolean SOUTH = BlockSprawling.SOUTH;
    public static final BlockStateBoolean WEST = BlockSprawling.WEST;
    public static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockSprawling.PROPERTY_BY_DIRECTION.entrySet().stream().filter((entry) -> {
        return entry.getKey() != EnumDirection.DOWN;
    }).collect(SystemUtils.toMap());
    protected static final float AABB_OFFSET = 1.0F;
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private final Map<IBlockData, VoxelShape> shapesCache;

    public BlockVine(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(UP, Boolean.valueOf(false)).set(NORTH, Boolean.valueOf(false)).set(EAST, Boolean.valueOf(false)).set(SOUTH, Boolean.valueOf(false)).set(WEST, Boolean.valueOf(false)));
        this.shapesCache = ImmutableMap.copyOf(this.stateDefinition.getPossibleStates().stream().collect(Collectors.toMap(Function.identity(), BlockVine::calculateShape)));
    }

    private static VoxelShape calculateShape(IBlockData state) {
        VoxelShape voxelShape = VoxelShapes.empty();
        if (state.get(UP)) {
            voxelShape = UP_AABB;
        }

        if (state.get(NORTH)) {
            voxelShape = VoxelShapes.or(voxelShape, NORTH_AABB);
        }

        if (state.get(SOUTH)) {
            voxelShape = VoxelShapes.or(voxelShape, SOUTH_AABB);
        }

        if (state.get(EAST)) {
            voxelShape = VoxelShapes.or(voxelShape, EAST_AABB);
        }

        if (state.get(WEST)) {
            voxelShape = VoxelShapes.or(voxelShape, WEST_AABB);
        }

        return voxelShape.isEmpty() ? VoxelShapes.block() : voxelShape;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapesCache.get(state);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return true;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return this.hasFaces(this.getUpdatedState(state, world, pos));
    }

    private boolean hasFaces(IBlockData state) {
        return this.countFaces(state) > 0;
    }

    private int countFaces(IBlockData state) {
        int i = 0;

        for(BlockStateBoolean booleanProperty : PROPERTY_BY_DIRECTION.values()) {
            if (state.get(booleanProperty)) {
                ++i;
            }
        }

        return i;
    }

    private boolean canSupportAtFace(IBlockAccess world, BlockPosition pos, EnumDirection side) {
        if (side == EnumDirection.DOWN) {
            return false;
        } else {
            BlockPosition blockPos = pos.relative(side);
            if (isAcceptableNeighbour(world, blockPos, side)) {
                return true;
            } else if (side.getAxis() == EnumDirection.EnumAxis.Y) {
                return false;
            } else {
                BlockStateBoolean booleanProperty = PROPERTY_BY_DIRECTION.get(side);
                IBlockData blockState = world.getType(pos.above());
                return blockState.is(this) && blockState.get(booleanProperty);
            }
        }
    }

    public static boolean isAcceptableNeighbour(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        IBlockData blockState = world.getType(pos);
        return Block.isFaceFull(blockState.getCollisionShape(world, pos), direction.opposite());
    }

    private IBlockData getUpdatedState(IBlockData state, IBlockAccess world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        if (state.get(UP)) {
            state = state.set(UP, Boolean.valueOf(isAcceptableNeighbour(world, blockPos, EnumDirection.DOWN)));
        }

        IBlockData blockState = null;

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockStateBoolean booleanProperty = getDirection(direction);
            if (state.get(booleanProperty)) {
                boolean bl = this.canSupportAtFace(world, pos, direction);
                if (!bl) {
                    if (blockState == null) {
                        blockState = world.getType(blockPos);
                    }

                    bl = blockState.is(this) && blockState.get(booleanProperty);
                }

                state = state.set(booleanProperty, Boolean.valueOf(bl));
            }
        }

        return state;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.DOWN) {
            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            IBlockData blockState = this.getUpdatedState(state, world, pos);
            return !this.hasFaces(blockState) ? Blocks.AIR.getBlockData() : blockState;
        }
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (random.nextInt(4) == 0) {
            EnumDirection direction = EnumDirection.getRandom(random);
            BlockPosition blockPos = pos.above();
            if (direction.getAxis().isHorizontal() && !state.get(getDirection(direction))) {
                if (this.canSpread(world, pos)) {
                    BlockPosition blockPos2 = pos.relative(direction);
                    IBlockData blockState = world.getType(blockPos2);
                    if (blockState.isAir()) {
                        EnumDirection direction2 = direction.getClockWise();
                        EnumDirection direction3 = direction.getCounterClockWise();
                        boolean bl = state.get(getDirection(direction2));
                        boolean bl2 = state.get(getDirection(direction3));
                        BlockPosition blockPos3 = blockPos2.relative(direction2);
                        BlockPosition blockPos4 = blockPos2.relative(direction3);
                        if (bl && isAcceptableNeighbour(world, blockPos3, direction2)) {
                            world.setTypeAndData(blockPos2, this.getBlockData().set(getDirection(direction2), Boolean.valueOf(true)), 2);
                        } else if (bl2 && isAcceptableNeighbour(world, blockPos4, direction3)) {
                            world.setTypeAndData(blockPos2, this.getBlockData().set(getDirection(direction3), Boolean.valueOf(true)), 2);
                        } else {
                            EnumDirection direction4 = direction.opposite();
                            if (bl && world.isEmpty(blockPos3) && isAcceptableNeighbour(world, pos.relative(direction2), direction4)) {
                                world.setTypeAndData(blockPos3, this.getBlockData().set(getDirection(direction4), Boolean.valueOf(true)), 2);
                            } else if (bl2 && world.isEmpty(blockPos4) && isAcceptableNeighbour(world, pos.relative(direction3), direction4)) {
                                world.setTypeAndData(blockPos4, this.getBlockData().set(getDirection(direction4), Boolean.valueOf(true)), 2);
                            } else if ((double)random.nextFloat() < 0.05D && isAcceptableNeighbour(world, blockPos2.above(), EnumDirection.UP)) {
                                world.setTypeAndData(blockPos2, this.getBlockData().set(UP, Boolean.valueOf(true)), 2);
                            }
                        }
                    } else if (isAcceptableNeighbour(world, blockPos2, direction)) {
                        world.setTypeAndData(pos, state.set(getDirection(direction), Boolean.valueOf(true)), 2);
                    }

                }
            } else {
                if (direction == EnumDirection.UP && pos.getY() < world.getMaxBuildHeight() - 1) {
                    if (this.canSupportAtFace(world, pos, direction)) {
                        world.setTypeAndData(pos, state.set(UP, Boolean.valueOf(true)), 2);
                        return;
                    }

                    if (world.isEmpty(blockPos)) {
                        if (!this.canSpread(world, pos)) {
                            return;
                        }

                        IBlockData blockState2 = state;

                        for(EnumDirection direction5 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                            if (random.nextBoolean() || !isAcceptableNeighbour(world, blockPos.relative(direction5), direction5)) {
                                blockState2 = blockState2.set(getDirection(direction5), Boolean.valueOf(false));
                            }
                        }

                        if (this.canSpread(blockState2)) {
                            world.setTypeAndData(blockPos, blockState2, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > world.getMinBuildHeight()) {
                    BlockPosition blockPos5 = pos.below();
                    IBlockData blockState3 = world.getType(blockPos5);
                    if (blockState3.isAir() || blockState3.is(this)) {
                        IBlockData blockState4 = blockState3.isAir() ? this.getBlockData() : blockState3;
                        IBlockData blockState5 = this.copyRandomFaces(state, blockState4, random);
                        if (blockState4 != blockState5 && this.canSpread(blockState5)) {
                            world.setTypeAndData(blockPos5, blockState5, 2);
                        }
                    }
                }

            }
        }
    }

    private IBlockData copyRandomFaces(IBlockData above, IBlockData state, Random random) {
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (random.nextBoolean()) {
                BlockStateBoolean booleanProperty = getDirection(direction);
                if (above.get(booleanProperty)) {
                    state = state.set(booleanProperty, Boolean.valueOf(true));
                }
            }
        }

        return state;
    }

    private boolean canSpread(IBlockData state) {
        return state.get(NORTH) || state.get(EAST) || state.get(SOUTH) || state.get(WEST);
    }

    private boolean canSpread(IBlockAccess world, BlockPosition pos) {
        int i = 4;
        Iterable<BlockPosition> iterable = BlockPosition.betweenClosed(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4);
        int j = 5;

        for(BlockPosition blockPos : iterable) {
            if (world.getType(blockPos).is(this)) {
                --j;
                if (j <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        IBlockData blockState = context.getWorld().getType(context.getClickPosition());
        if (blockState.is(this)) {
            return this.countFaces(blockState) < PROPERTY_BY_DIRECTION.size();
        } else {
            return super.canBeReplaced(state, context);
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition());
        boolean bl = blockState.is(this);
        IBlockData blockState2 = bl ? blockState : this.getBlockData();

        for(EnumDirection direction : ctx.getNearestLookingDirections()) {
            if (direction != EnumDirection.DOWN) {
                BlockStateBoolean booleanProperty = getDirection(direction);
                boolean bl2 = bl && blockState.get(booleanProperty);
                if (!bl2 && this.canSupportAtFace(ctx.getWorld(), ctx.getClickPosition(), direction)) {
                    return blockState2.set(booleanProperty, Boolean.valueOf(true));
                }
            }
        }

        return bl ? blockState2 : null;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.set(NORTH, state.get(SOUTH)).set(EAST, state.get(WEST)).set(SOUTH, state.get(NORTH)).set(WEST, state.get(EAST));
        case COUNTERCLOCKWISE_90:
            return state.set(NORTH, state.get(EAST)).set(EAST, state.get(SOUTH)).set(SOUTH, state.get(WEST)).set(WEST, state.get(NORTH));
        case CLOCKWISE_90:
            return state.set(NORTH, state.get(WEST)).set(EAST, state.get(NORTH)).set(SOUTH, state.get(EAST)).set(WEST, state.get(SOUTH));
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.set(NORTH, state.get(SOUTH)).set(SOUTH, state.get(NORTH));
        case FRONT_BACK:
            return state.set(EAST, state.get(WEST)).set(WEST, state.get(EAST));
        default:
            return super.mirror(state, mirror);
        }
    }

    public static BlockStateBoolean getDirection(EnumDirection direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }
}
