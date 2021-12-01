package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockMultiface extends Block {
    private static final float AABB_OFFSET = 1.0F;
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockSprawling.PROPERTY_BY_DIRECTION;
    private static final Map<EnumDirection, VoxelShape> SHAPE_BY_DIRECTION = SystemUtils.make(Maps.newEnumMap(EnumDirection.class), (shapes) -> {
        shapes.put(EnumDirection.NORTH, NORTH_AABB);
        shapes.put(EnumDirection.EAST, EAST_AABB);
        shapes.put(EnumDirection.SOUTH, SOUTH_AABB);
        shapes.put(EnumDirection.WEST, WEST_AABB);
        shapes.put(EnumDirection.UP, UP_AABB);
        shapes.put(EnumDirection.DOWN, DOWN_AABB);
    });
    protected static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    private final ImmutableMap<IBlockData, VoxelShape> shapesCache;
    private final boolean canRotate;
    private final boolean canMirrorX;
    private final boolean canMirrorZ;

    public BlockMultiface(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(getDefaultMultifaceState(this.stateDefinition));
        this.shapesCache = this.getShapeForEachState(BlockMultiface::calculateMultifaceShape);
        this.canRotate = EnumDirection.EnumDirectionLimit.HORIZONTAL.stream().allMatch(this::isFaceSupported);
        this.canMirrorX = EnumDirection.EnumDirectionLimit.HORIZONTAL.stream().filter(EnumDirection.EnumAxis.X).filter(this::isFaceSupported).count() % 2L == 0L;
        this.canMirrorZ = EnumDirection.EnumDirectionLimit.HORIZONTAL.stream().filter(EnumDirection.EnumAxis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
    }

    protected boolean isFaceSupported(EnumDirection direction) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        for(EnumDirection direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                builder.add(getFaceProperty(direction));
            }
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!hasAnyFace(state)) {
            return Blocks.AIR.getBlockData();
        } else {
            return hasFace(state, direction) && !canAttachTo(world, direction, neighborPos, neighborState) ? removeFace(state, getFaceProperty(direction)) : state;
        }
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapesCache.get(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        boolean bl = false;

        for(EnumDirection direction : DIRECTIONS) {
            if (hasFace(state, direction)) {
                BlockPosition blockPos = pos.relative(direction);
                if (!canAttachTo(world, direction, blockPos, world.getType(blockPos))) {
                    return false;
                }

                bl = true;
            }
        }

        return bl;
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return hasAnyVacantFace(state);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        World level = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        return Arrays.stream(ctx.getNearestLookingDirections()).map((direction) -> {
            return this.getStateForPlacement(blockState, level, blockPos, direction);
        }).filter(Objects::nonNull).findFirst().orElse((IBlockData)null);
    }

    @Nullable
    public IBlockData getStateForPlacement(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        if (!this.isFaceSupported(direction)) {
            return null;
        } else {
            IBlockData blockState;
            if (state.is(this)) {
                if (hasFace(state, direction)) {
                    return null;
                }

                blockState = state;
            } else if (this.isWaterloggable() && state.getFluid().isSourceOfType(FluidTypes.WATER)) {
                blockState = this.getBlockData().set(BlockProperties.WATERLOGGED, Boolean.valueOf(true));
            } else {
                blockState = this.getBlockData();
            }

            BlockPosition blockPos = pos.relative(direction);
            return canAttachTo(world, direction, blockPos, world.getType(blockPos)) ? blockState.set(getFaceProperty(direction), Boolean.valueOf(true)) : null;
        }
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return !this.canRotate ? state : this.mapDirections(state, rotation::rotate);
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        if (mirror == EnumBlockMirror.FRONT_BACK && !this.canMirrorX) {
            return state;
        } else {
            return mirror == EnumBlockMirror.LEFT_RIGHT && !this.canMirrorZ ? state : this.mapDirections(state, mirror::mirror);
        }
    }

    private IBlockData mapDirections(IBlockData state, Function<EnumDirection, EnumDirection> mirror) {
        IBlockData blockState = state;

        for(EnumDirection direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                blockState = blockState.set(getFaceProperty(mirror.apply(direction)), state.get(getFaceProperty(direction)));
            }
        }

        return blockState;
    }

    public boolean spreadFromRandomFaceTowardRandomDirection(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        List<EnumDirection> list = Lists.newArrayList(DIRECTIONS);
        Collections.shuffle(list);
        return list.stream().filter((from) -> {
            return hasFace(state, from);
        }).anyMatch((to) -> {
            return this.spreadFromFaceTowardRandomDirection(state, world, pos, to, random, false);
        });
    }

    public boolean spreadFromFaceTowardRandomDirection(IBlockData state, GeneratorAccess world, BlockPosition pos, EnumDirection from, Random random, boolean postProcess) {
        List<EnumDirection> list = Arrays.asList(DIRECTIONS);
        Collections.shuffle(list, random);
        return list.stream().anyMatch((to) -> {
            return this.spreadFromFaceTowardDirection(state, world, pos, from, to, postProcess);
        });
    }

    public boolean spreadFromFaceTowardDirection(IBlockData state, GeneratorAccess world, BlockPosition pos, EnumDirection from, EnumDirection to, boolean postProcess) {
        Optional<Pair<BlockPosition, EnumDirection>> optional = this.getSpreadFromFaceTowardDirection(state, world, pos, from, to);
        if (optional.isPresent()) {
            Pair<BlockPosition, EnumDirection> pair = optional.get();
            return this.spreadToFace(world, pair.getFirst(), pair.getSecond(), postProcess);
        } else {
            return false;
        }
    }

    protected boolean canSpread(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection from) {
        return Stream.of(DIRECTIONS).anyMatch((to) -> {
            return this.getSpreadFromFaceTowardDirection(state, world, pos, from, to).isPresent();
        });
    }

    private Optional<Pair<BlockPosition, EnumDirection>> getSpreadFromFaceTowardDirection(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection from, EnumDirection to) {
        if (to.getAxis() != from.getAxis() && hasFace(state, from) && !hasFace(state, to)) {
            if (this.canSpreadToFace(world, pos, to)) {
                return Optional.of(Pair.of(pos, to));
            } else {
                BlockPosition blockPos = pos.relative(to);
                if (this.canSpreadToFace(world, blockPos, from)) {
                    return Optional.of(Pair.of(blockPos, from));
                } else {
                    BlockPosition blockPos2 = blockPos.relative(from);
                    EnumDirection direction = to.opposite();
                    return this.canSpreadToFace(world, blockPos2, direction) ? Optional.of(Pair.of(blockPos2, direction)) : Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private boolean canSpreadToFace(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        IBlockData blockState = world.getType(pos);
        if (!this.canSpreadInto(blockState)) {
            return false;
        } else {
            IBlockData blockState2 = this.getStateForPlacement(blockState, world, pos, direction);
            return blockState2 != null;
        }
    }

    private boolean spreadToFace(GeneratorAccess world, BlockPosition pos, EnumDirection direction, boolean postProcess) {
        IBlockData blockState = world.getType(pos);
        IBlockData blockState2 = this.getStateForPlacement(blockState, world, pos, direction);
        if (blockState2 != null) {
            if (postProcess) {
                world.getChunk(pos).markPosForPostprocessing(pos);
            }

            return world.setTypeAndData(pos, blockState2, 2);
        } else {
            return false;
        }
    }

    private boolean canSpreadInto(IBlockData state) {
        return state.isAir() || state.is(this) || state.is(Blocks.WATER) && state.getFluid().isSource();
    }

    private static boolean hasFace(IBlockData state, EnumDirection direction) {
        BlockStateBoolean booleanProperty = getFaceProperty(direction);
        return state.hasProperty(booleanProperty) && state.get(booleanProperty);
    }

    private static boolean canAttachTo(IBlockAccess world, EnumDirection direction, BlockPosition pos, IBlockData state) {
        return Block.isFaceFull(state.getCollisionShape(world, pos), direction.opposite());
    }

    private boolean isWaterloggable() {
        return this.stateDefinition.getProperties().contains(BlockProperties.WATERLOGGED);
    }

    private static IBlockData removeFace(IBlockData state, BlockStateBoolean direction) {
        IBlockData blockState = state.set(direction, Boolean.valueOf(false));
        return hasAnyFace(blockState) ? blockState : Blocks.AIR.getBlockData();
    }

    public static BlockStateBoolean getFaceProperty(EnumDirection direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }

    private static IBlockData getDefaultMultifaceState(BlockStateList<Block, IBlockData> stateManager) {
        IBlockData blockState = stateManager.getBlockData();

        for(BlockStateBoolean booleanProperty : PROPERTY_BY_DIRECTION.values()) {
            if (blockState.hasProperty(booleanProperty)) {
                blockState = blockState.set(booleanProperty, Boolean.valueOf(false));
            }
        }

        return blockState;
    }

    private static VoxelShape calculateMultifaceShape(IBlockData state) {
        VoxelShape voxelShape = VoxelShapes.empty();

        for(EnumDirection direction : DIRECTIONS) {
            if (hasFace(state, direction)) {
                voxelShape = VoxelShapes.or(voxelShape, SHAPE_BY_DIRECTION.get(direction));
            }
        }

        return voxelShape.isEmpty() ? VoxelShapes.block() : voxelShape;
    }

    protected static boolean hasAnyFace(IBlockData state) {
        return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
            return hasFace(state, direction);
        });
    }

    private static boolean hasAnyVacantFace(IBlockData state) {
        return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
            return !hasFace(state, direction);
        });
    }
}
