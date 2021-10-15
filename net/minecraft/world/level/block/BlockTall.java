package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockTall extends Block implements IBlockWaterlogged {
    public static final BlockStateBoolean NORTH = BlockSprawling.NORTH;
    public static final BlockStateBoolean EAST = BlockSprawling.EAST;
    public static final BlockStateBoolean SOUTH = BlockSprawling.SOUTH;
    public static final BlockStateBoolean WEST = BlockSprawling.WEST;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = BlockSprawling.PROPERTY_BY_DIRECTION.entrySet().stream().filter((entry) -> {
        return entry.getKey().getAxis().isHorizontal();
    }).collect(SystemUtils.toMap());
    protected final VoxelShape[] collisionShapeByIndex;
    protected final VoxelShape[] shapeByIndex;
    private final Object2IntMap<IBlockData> stateToIndex = new Object2IntOpenHashMap<>();

    protected BlockTall(float radius1, float radius2, float boundingHeight1, float boundingHeight2, float collisionHeight, BlockBase.Info settings) {
        super(settings);
        this.collisionShapeByIndex = this.makeShapes(radius1, radius2, collisionHeight, 0.0F, collisionHeight);
        this.shapeByIndex = this.makeShapes(radius1, radius2, boundingHeight1, 0.0F, boundingHeight2);

        for(IBlockData blockState : this.stateDefinition.getPossibleStates()) {
            this.getAABBIndex(blockState);
        }

    }

    protected VoxelShape[] makeShapes(float radius1, float radius2, float height1, float offset2, float height2) {
        float f = 8.0F - radius1;
        float g = 8.0F + radius1;
        float h = 8.0F - radius2;
        float i = 8.0F + radius2;
        VoxelShape voxelShape = Block.box((double)f, 0.0D, (double)f, (double)g, (double)height1, (double)g);
        VoxelShape voxelShape2 = Block.box((double)h, (double)offset2, 0.0D, (double)i, (double)height2, (double)i);
        VoxelShape voxelShape3 = Block.box((double)h, (double)offset2, (double)h, (double)i, (double)height2, 16.0D);
        VoxelShape voxelShape4 = Block.box(0.0D, (double)offset2, (double)h, (double)i, (double)height2, (double)i);
        VoxelShape voxelShape5 = Block.box((double)h, (double)offset2, (double)h, 16.0D, (double)height2, (double)i);
        VoxelShape voxelShape6 = VoxelShapes.or(voxelShape2, voxelShape5);
        VoxelShape voxelShape7 = VoxelShapes.or(voxelShape3, voxelShape4);
        VoxelShape[] voxelShapes = new VoxelShape[]{VoxelShapes.empty(), voxelShape3, voxelShape4, voxelShape7, voxelShape2, VoxelShapes.or(voxelShape3, voxelShape2), VoxelShapes.or(voxelShape4, voxelShape2), VoxelShapes.or(voxelShape7, voxelShape2), voxelShape5, VoxelShapes.or(voxelShape3, voxelShape5), VoxelShapes.or(voxelShape4, voxelShape5), VoxelShapes.or(voxelShape7, voxelShape5), voxelShape6, VoxelShapes.or(voxelShape3, voxelShape6), VoxelShapes.or(voxelShape4, voxelShape6), VoxelShapes.or(voxelShape7, voxelShape6)};

        for(int j = 0; j < 16; ++j) {
            voxelShapes[j] = VoxelShapes.or(voxelShape, voxelShapes[j]);
        }

        return voxelShapes;
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return !state.get(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapeByIndex[this.getAABBIndex(state)];
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.collisionShapeByIndex[this.getAABBIndex(state)];
    }

    private static int indexFor(EnumDirection dir) {
        return 1 << dir.get2DRotationValue();
    }

    protected int getAABBIndex(IBlockData state) {
        return this.stateToIndex.computeIntIfAbsent(state, (blockState) -> {
            int i = 0;
            if (blockState.get(NORTH)) {
                i |= indexFor(EnumDirection.NORTH);
            }

            if (blockState.get(EAST)) {
                i |= indexFor(EnumDirection.EAST);
            }

            if (blockState.get(SOUTH)) {
                i |= indexFor(EnumDirection.SOUTH);
            }

            if (blockState.get(WEST)) {
                i |= indexFor(EnumDirection.WEST);
            }

            return i;
        });
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
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
}
