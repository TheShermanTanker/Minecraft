package net.minecraft.world.level.block;

import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyHalf;
import net.minecraft.world.level.block.state.properties.BlockPropertyStairsShape;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockStairs extends Block implements IBlockWaterlogged {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateEnum<BlockPropertyHalf> HALF = BlockProperties.HALF;
    public static final BlockStateEnum<BlockPropertyStairsShape> SHAPE = BlockProperties.STAIRS_SHAPE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape TOP_AABB = BlockStepAbstract.TOP_AABB;
    protected static final VoxelShape BOTTOM_AABB = BlockStepAbstract.BOTTOM_AABB;
    protected static final VoxelShape OCTET_NNN = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D);
    protected static final VoxelShape OCTET_NNP = Block.box(0.0D, 0.0D, 8.0D, 8.0D, 8.0D, 16.0D);
    protected static final VoxelShape OCTET_NPN = Block.box(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D);
    protected static final VoxelShape OCTET_NPP = Block.box(0.0D, 8.0D, 8.0D, 8.0D, 16.0D, 16.0D);
    protected static final VoxelShape OCTET_PNN = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D);
    protected static final VoxelShape OCTET_PNP = Block.box(8.0D, 0.0D, 8.0D, 16.0D, 8.0D, 16.0D);
    protected static final VoxelShape OCTET_PPN = Block.box(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    protected static final VoxelShape OCTET_PPP = Block.box(8.0D, 8.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape[] TOP_SHAPES = makeShapes(TOP_AABB, OCTET_NNN, OCTET_PNN, OCTET_NNP, OCTET_PNP);
    protected static final VoxelShape[] BOTTOM_SHAPES = makeShapes(BOTTOM_AABB, OCTET_NPN, OCTET_PPN, OCTET_NPP, OCTET_PPP);
    private static final int[] SHAPE_BY_STATE = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};
    private final Block base;
    private final IBlockData baseState;

    private static VoxelShape[] makeShapes(VoxelShape base, VoxelShape northWest, VoxelShape northEast, VoxelShape southWest, VoxelShape southEast) {
        return IntStream.range(0, 16).mapToObj((i) -> {
            return makeStairShape(i, base, northWest, northEast, southWest, southEast);
        }).toArray((i) -> {
            return new VoxelShape[i];
        });
    }

    private static VoxelShape makeStairShape(int i, VoxelShape base, VoxelShape northWest, VoxelShape northEast, VoxelShape southWest, VoxelShape southEast) {
        VoxelShape voxelShape = base;
        if ((i & 1) != 0) {
            voxelShape = VoxelShapes.or(base, northWest);
        }

        if ((i & 2) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, northEast);
        }

        if ((i & 4) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, southWest);
        }

        if ((i & 8) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, southEast);
        }

        return voxelShape;
    }

    protected BlockStairs(IBlockData baseBlockState, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(HALF, BlockPropertyHalf.BOTTOM).set(SHAPE, BlockPropertyStairsShape.STRAIGHT).set(WATERLOGGED, Boolean.valueOf(false)));
        this.base = baseBlockState.getBlock();
        this.baseState = baseBlockState;
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return (state.get(HALF) == BlockPropertyHalf.TOP ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_BY_STATE[this.getShapeIndex(state)]];
    }

    private int getShapeIndex(IBlockData state) {
        return state.get(SHAPE).ordinal() * 4 + state.get(FACING).get2DRotationValue();
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        this.base.animateTick(state, world, pos, random);
    }

    @Override
    public void attack(IBlockData state, World world, BlockPosition pos, EntityHuman player) {
        this.baseState.attack(world, pos, player);
    }

    @Override
    public void postBreak(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        this.base.postBreak(world, pos, state);
    }

    @Override
    public float getDurability() {
        return this.base.getDurability();
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!state.is(state.getBlock())) {
            this.baseState.doPhysics(world, pos, Blocks.AIR, pos, false);
            this.base.onPlace(this.baseState, world, pos, oldState, false);
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            this.baseState.remove(world, pos, newState, moved);
        }
    }

    @Override
    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
        this.base.stepOn(world, pos, state, entity);
    }

    @Override
    public boolean isTicking(IBlockData state) {
        return this.base.isTicking(state);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.base.tick(state, world, pos, random);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.base.tickAlways(state, world, pos, random);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        return this.baseState.interact(world, player, hand, hit);
    }

    @Override
    public void wasExploded(World world, BlockPosition pos, Explosion explosion) {
        this.base.wasExploded(world, pos, explosion);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace();
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(blockPos);
        IBlockData blockState = this.getBlockData().set(FACING, ctx.getHorizontalDirection()).set(HALF, direction != EnumDirection.DOWN && (direction == EnumDirection.UP || !(ctx.getPos().y - (double)blockPos.getY() > 0.5D)) ? BlockPropertyHalf.BOTTOM : BlockPropertyHalf.TOP).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
        return blockState.set(SHAPE, getStairsShape(blockState, ctx.getWorld(), blockPos));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction.getAxis().isHorizontal() ? state.set(SHAPE, getStairsShape(state, world, pos)) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    private static BlockPropertyStairsShape getStairsShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        IBlockData blockState = world.getType(pos.relative(direction));
        if (isStairs(blockState) && state.get(HALF) == blockState.get(HALF)) {
            EnumDirection direction2 = blockState.get(FACING);
            if (direction2.getAxis() != state.get(FACING).getAxis() && canTakeShape(state, world, pos, direction2.opposite())) {
                if (direction2 == direction.getCounterClockWise()) {
                    return BlockPropertyStairsShape.OUTER_LEFT;
                }

                return BlockPropertyStairsShape.OUTER_RIGHT;
            }
        }

        IBlockData blockState2 = world.getType(pos.relative(direction.opposite()));
        if (isStairs(blockState2) && state.get(HALF) == blockState2.get(HALF)) {
            EnumDirection direction3 = blockState2.get(FACING);
            if (direction3.getAxis() != state.get(FACING).getAxis() && canTakeShape(state, world, pos, direction3)) {
                if (direction3 == direction.getCounterClockWise()) {
                    return BlockPropertyStairsShape.INNER_LEFT;
                }

                return BlockPropertyStairsShape.INNER_RIGHT;
            }
        }

        return BlockPropertyStairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection dir) {
        IBlockData blockState = world.getType(pos.relative(dir));
        return !isStairs(blockState) || blockState.get(FACING) != state.get(FACING) || blockState.get(HALF) != state.get(HALF);
    }

    public static boolean isStairs(IBlockData state) {
        return state.getBlock() instanceof BlockStairs;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        EnumDirection direction = state.get(FACING);
        BlockPropertyStairsShape stairsShape = state.get(SHAPE);
        switch(mirror) {
        case LEFT_RIGHT:
            if (direction.getAxis() == EnumDirection.EnumAxis.Z) {
                switch(stairsShape) {
                case INNER_LEFT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.INNER_RIGHT);
                case INNER_RIGHT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.INNER_LEFT);
                case OUTER_LEFT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.OUTER_RIGHT);
                case OUTER_RIGHT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.OUTER_LEFT);
                default:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180);
                }
            }
            break;
        case FRONT_BACK:
            if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                switch(stairsShape) {
                case INNER_LEFT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.INNER_LEFT);
                case INNER_RIGHT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.INNER_RIGHT);
                case OUTER_LEFT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.OUTER_RIGHT);
                case OUTER_RIGHT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180).set(SHAPE, BlockPropertyStairsShape.OUTER_LEFT);
                case STRAIGHT:
                    return state.rotate(EnumBlockRotation.CLOCKWISE_180);
                }
            }
        }

        return super.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, HALF, SHAPE, WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
