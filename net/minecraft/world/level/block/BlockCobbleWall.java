package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyWallHeight;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockCobbleWall extends Block implements IBlockWaterlogged {
    public static final BlockStateBoolean UP = BlockProperties.UP;
    public static final BlockStateEnum<BlockPropertyWallHeight> EAST_WALL = BlockProperties.EAST_WALL;
    public static final BlockStateEnum<BlockPropertyWallHeight> NORTH_WALL = BlockProperties.NORTH_WALL;
    public static final BlockStateEnum<BlockPropertyWallHeight> SOUTH_WALL = BlockProperties.SOUTH_WALL;
    public static final BlockStateEnum<BlockPropertyWallHeight> WEST_WALL = BlockProperties.WEST_WALL;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private final Map<IBlockData, VoxelShape> shapeByIndex;
    private final Map<IBlockData, VoxelShape> collisionShapeByIndex;
    private static final int WALL_WIDTH = 3;
    private static final int WALL_HEIGHT = 14;
    private static final int POST_WIDTH = 4;
    private static final int POST_COVER_WIDTH = 1;
    private static final int WALL_COVER_START = 7;
    private static final int WALL_COVER_END = 9;
    private static final VoxelShape POST_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape NORTH_TEST = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SOUTH_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_TEST = Block.box(0.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape EAST_TEST = Block.box(7.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    public BlockCobbleWall(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(UP, Boolean.valueOf(true)).set(NORTH_WALL, BlockPropertyWallHeight.NONE).set(EAST_WALL, BlockPropertyWallHeight.NONE).set(SOUTH_WALL, BlockPropertyWallHeight.NONE).set(WEST_WALL, BlockPropertyWallHeight.NONE).set(WATERLOGGED, Boolean.valueOf(false)));
        this.shapeByIndex = this.makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
        this.collisionShapeByIndex = this.makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
    }

    private static VoxelShape applyWallShape(VoxelShape voxelShape, BlockPropertyWallHeight wallSide, VoxelShape voxelShape2, VoxelShape voxelShape3) {
        if (wallSide == BlockPropertyWallHeight.TALL) {
            return VoxelShapes.or(voxelShape, voxelShape3);
        } else {
            return wallSide == BlockPropertyWallHeight.LOW ? VoxelShapes.or(voxelShape, voxelShape2) : voxelShape;
        }
    }

    private Map<IBlockData, VoxelShape> makeShapes(float f, float g, float h, float i, float j, float k) {
        float l = 8.0F - f;
        float m = 8.0F + f;
        float n = 8.0F - g;
        float o = 8.0F + g;
        VoxelShape voxelShape = Block.box((double)l, 0.0D, (double)l, (double)m, (double)h, (double)m);
        VoxelShape voxelShape2 = Block.box((double)n, (double)i, 0.0D, (double)o, (double)j, (double)o);
        VoxelShape voxelShape3 = Block.box((double)n, (double)i, (double)n, (double)o, (double)j, 16.0D);
        VoxelShape voxelShape4 = Block.box(0.0D, (double)i, (double)n, (double)o, (double)j, (double)o);
        VoxelShape voxelShape5 = Block.box((double)n, (double)i, (double)n, 16.0D, (double)j, (double)o);
        VoxelShape voxelShape6 = Block.box((double)n, (double)i, 0.0D, (double)o, (double)k, (double)o);
        VoxelShape voxelShape7 = Block.box((double)n, (double)i, (double)n, (double)o, (double)k, 16.0D);
        VoxelShape voxelShape8 = Block.box(0.0D, (double)i, (double)n, (double)o, (double)k, (double)o);
        VoxelShape voxelShape9 = Block.box((double)n, (double)i, (double)n, 16.0D, (double)k, (double)o);
        Builder<IBlockData, VoxelShape> builder = ImmutableMap.builder();

        for(Boolean boolean_ : UP.getValues()) {
            for(BlockPropertyWallHeight wallSide : EAST_WALL.getValues()) {
                for(BlockPropertyWallHeight wallSide2 : NORTH_WALL.getValues()) {
                    for(BlockPropertyWallHeight wallSide3 : WEST_WALL.getValues()) {
                        for(BlockPropertyWallHeight wallSide4 : SOUTH_WALL.getValues()) {
                            VoxelShape voxelShape10 = VoxelShapes.empty();
                            voxelShape10 = applyWallShape(voxelShape10, wallSide, voxelShape5, voxelShape9);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide3, voxelShape4, voxelShape8);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide2, voxelShape2, voxelShape6);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide4, voxelShape3, voxelShape7);
                            if (boolean_) {
                                voxelShape10 = VoxelShapes.or(voxelShape10, voxelShape);
                            }

                            IBlockData blockState = this.getBlockData().set(UP, boolean_).set(EAST_WALL, wallSide).set(WEST_WALL, wallSide3).set(NORTH_WALL, wallSide2).set(SOUTH_WALL, wallSide4);
                            builder.put(blockState.set(WATERLOGGED, Boolean.valueOf(false)), voxelShape10);
                            builder.put(blockState.set(WATERLOGGED, Boolean.valueOf(true)), voxelShape10);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapeByIndex.get(state);
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.collisionShapeByIndex.get(state);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    private boolean connectsTo(IBlockData state, boolean faceFullSquare, EnumDirection side) {
        Block block = state.getBlock();
        boolean bl = block instanceof BlockFenceGate && BlockFenceGate.connectsToDirection(state, side);
        return state.is(TagsBlock.WALLS) || !isExceptionForConnection(state) && faceFullSquare || block instanceof BlockIronBars || bl;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        BlockPosition blockPos2 = blockPos.north();
        BlockPosition blockPos3 = blockPos.east();
        BlockPosition blockPos4 = blockPos.south();
        BlockPosition blockPos5 = blockPos.west();
        BlockPosition blockPos6 = blockPos.above();
        IBlockData blockState = levelReader.getType(blockPos2);
        IBlockData blockState2 = levelReader.getType(blockPos3);
        IBlockData blockState3 = levelReader.getType(blockPos4);
        IBlockData blockState4 = levelReader.getType(blockPos5);
        IBlockData blockState5 = levelReader.getType(blockPos6);
        boolean bl = this.connectsTo(blockState, blockState.isFaceSturdy(levelReader, blockPos2, EnumDirection.SOUTH), EnumDirection.SOUTH);
        boolean bl2 = this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos3, EnumDirection.WEST), EnumDirection.WEST);
        boolean bl3 = this.connectsTo(blockState3, blockState3.isFaceSturdy(levelReader, blockPos4, EnumDirection.NORTH), EnumDirection.NORTH);
        boolean bl4 = this.connectsTo(blockState4, blockState4.isFaceSturdy(levelReader, blockPos5, EnumDirection.EAST), EnumDirection.EAST);
        IBlockData blockState6 = this.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
        return this.updateShape(levelReader, blockState6, blockPos6, blockState5, bl, bl2, bl3, bl4);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        if (direction == EnumDirection.DOWN) {
            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return direction == EnumDirection.UP ? this.topUpdate(world, state, neighborPos, neighborState) : this.sideUpdate(world, pos, state, neighborPos, neighborState, direction);
        }
    }

    private static boolean isConnected(IBlockData blockState, IBlockState<BlockPropertyWallHeight> property) {
        return blockState.get(property) != BlockPropertyWallHeight.NONE;
    }

    private static boolean isCovered(VoxelShape voxelShape, VoxelShape voxelShape2) {
        return !VoxelShapes.joinIsNotEmpty(voxelShape2, voxelShape, OperatorBoolean.ONLY_FIRST);
    }

    private IBlockData topUpdate(IWorldReader levelReader, IBlockData blockState, BlockPosition blockPos, IBlockData blockState2) {
        boolean bl = isConnected(blockState, NORTH_WALL);
        boolean bl2 = isConnected(blockState, EAST_WALL);
        boolean bl3 = isConnected(blockState, SOUTH_WALL);
        boolean bl4 = isConnected(blockState, WEST_WALL);
        return this.updateShape(levelReader, blockState, blockPos, blockState2, bl, bl2, bl3, bl4);
    }

    private IBlockData sideUpdate(IWorldReader levelReader, BlockPosition blockPos, IBlockData blockState, BlockPosition blockPos2, IBlockData blockState2, EnumDirection direction) {
        EnumDirection direction2 = direction.opposite();
        boolean bl = direction == EnumDirection.NORTH ? this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos2, direction2), direction2) : isConnected(blockState, NORTH_WALL);
        boolean bl2 = direction == EnumDirection.EAST ? this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos2, direction2), direction2) : isConnected(blockState, EAST_WALL);
        boolean bl3 = direction == EnumDirection.SOUTH ? this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos2, direction2), direction2) : isConnected(blockState, SOUTH_WALL);
        boolean bl4 = direction == EnumDirection.WEST ? this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos2, direction2), direction2) : isConnected(blockState, WEST_WALL);
        BlockPosition blockPos3 = blockPos.above();
        IBlockData blockState3 = levelReader.getType(blockPos3);
        return this.updateShape(levelReader, blockState, blockPos3, blockState3, bl, bl2, bl3, bl4);
    }

    private IBlockData updateShape(IWorldReader levelReader, IBlockData blockState, BlockPosition blockPos, IBlockData blockState2, boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        VoxelShape voxelShape = blockState2.getCollisionShape(levelReader, blockPos).getFaceShape(EnumDirection.DOWN);
        IBlockData blockState3 = this.updateSides(blockState, bl, bl2, bl3, bl4, voxelShape);
        return blockState3.set(UP, Boolean.valueOf(this.shouldRaisePost(blockState3, blockState2, voxelShape)));
    }

    private boolean shouldRaisePost(IBlockData blockState, IBlockData blockState2, VoxelShape voxelShape) {
        boolean bl = blockState2.getBlock() instanceof BlockCobbleWall && blockState2.get(UP);
        if (bl) {
            return true;
        } else {
            BlockPropertyWallHeight wallSide = blockState.get(NORTH_WALL);
            BlockPropertyWallHeight wallSide2 = blockState.get(SOUTH_WALL);
            BlockPropertyWallHeight wallSide3 = blockState.get(EAST_WALL);
            BlockPropertyWallHeight wallSide4 = blockState.get(WEST_WALL);
            boolean bl2 = wallSide2 == BlockPropertyWallHeight.NONE;
            boolean bl3 = wallSide4 == BlockPropertyWallHeight.NONE;
            boolean bl4 = wallSide3 == BlockPropertyWallHeight.NONE;
            boolean bl5 = wallSide == BlockPropertyWallHeight.NONE;
            boolean bl6 = bl5 && bl2 && bl3 && bl4 || bl5 != bl2 || bl3 != bl4;
            if (bl6) {
                return true;
            } else {
                boolean bl7 = wallSide == BlockPropertyWallHeight.TALL && wallSide2 == BlockPropertyWallHeight.TALL || wallSide3 == BlockPropertyWallHeight.TALL && wallSide4 == BlockPropertyWallHeight.TALL;
                if (bl7) {
                    return false;
                } else {
                    return blockState2.is(TagsBlock.WALL_POST_OVERRIDE) || isCovered(voxelShape, POST_TEST);
                }
            }
        }
    }

    private IBlockData updateSides(IBlockData blockState, boolean bl, boolean bl2, boolean bl3, boolean bl4, VoxelShape voxelShape) {
        return blockState.set(NORTH_WALL, this.makeWallState(bl, voxelShape, NORTH_TEST)).set(EAST_WALL, this.makeWallState(bl2, voxelShape, EAST_TEST)).set(SOUTH_WALL, this.makeWallState(bl3, voxelShape, SOUTH_TEST)).set(WEST_WALL, this.makeWallState(bl4, voxelShape, WEST_TEST));
    }

    private BlockPropertyWallHeight makeWallState(boolean bl, VoxelShape voxelShape, VoxelShape voxelShape2) {
        if (bl) {
            return isCovered(voxelShape, voxelShape2) ? BlockPropertyWallHeight.TALL : BlockPropertyWallHeight.LOW;
        } else {
            return BlockPropertyWallHeight.NONE;
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return !state.get(WATERLOGGED);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(UP, NORTH_WALL, EAST_WALL, WEST_WALL, SOUTH_WALL, WATERLOGGED);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.set(NORTH_WALL, state.get(SOUTH_WALL)).set(EAST_WALL, state.get(WEST_WALL)).set(SOUTH_WALL, state.get(NORTH_WALL)).set(WEST_WALL, state.get(EAST_WALL));
        case COUNTERCLOCKWISE_90:
            return state.set(NORTH_WALL, state.get(EAST_WALL)).set(EAST_WALL, state.get(SOUTH_WALL)).set(SOUTH_WALL, state.get(WEST_WALL)).set(WEST_WALL, state.get(NORTH_WALL));
        case CLOCKWISE_90:
            return state.set(NORTH_WALL, state.get(WEST_WALL)).set(EAST_WALL, state.get(NORTH_WALL)).set(SOUTH_WALL, state.get(EAST_WALL)).set(WEST_WALL, state.get(SOUTH_WALL));
        default:
            return state;
        }
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.set(NORTH_WALL, state.get(SOUTH_WALL)).set(SOUTH_WALL, state.get(NORTH_WALL));
        case FRONT_BACK:
            return state.set(EAST_WALL, state.get(WEST_WALL)).set(WEST_WALL, state.get(EAST_WALL));
        default:
            return super.mirror(state, mirror);
        }
    }
}
