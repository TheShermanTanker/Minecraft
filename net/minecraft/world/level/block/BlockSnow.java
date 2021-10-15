package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockSnow extends Block {
    public static final int MAX_HEIGHT = 8;
    public static final BlockStateInteger LAYERS = BlockProperties.LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{VoxelShapes.empty(), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    public static final int HEIGHT_IMPASSABLE = 5;

    protected BlockSnow(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LAYERS, Integer.valueOf(1)));
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return state.get(LAYERS) < 5;
        case WATER:
            return false;
        case AIR:
            return false;
        default:
            return false;
        }
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_LAYER[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_LAYER[state.get(LAYERS) - 1];
    }

    @Override
    public VoxelShape getBlockSupportShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return SHAPE_BY_LAYER[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getVisualShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_LAYER[state.get(LAYERS)];
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        if (!blockState.is(Blocks.ICE) && !blockState.is(Blocks.PACKED_ICE) && !blockState.is(Blocks.BARRIER)) {
            if (!blockState.is(Blocks.HONEY_BLOCK) && !blockState.is(Blocks.SOUL_SAND)) {
                return Block.isFaceFull(blockState.getCollisionShape(world, pos.below()), EnumDirection.UP) || blockState.is(this) && blockState.get(LAYERS) == 8;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getBrightness(EnumSkyBlock.BLOCK, pos) > 11) {
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }

    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        int i = state.get(LAYERS);
        if (context.getItemStack().is(this.getItem()) && i < 8) {
            if (context.replacingClickedOnBlock()) {
                return context.getClickedFace() == EnumDirection.UP;
            } else {
                return true;
            }
        } else {
            return i == 1;
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition());
        if (blockState.is(this)) {
            int i = blockState.get(LAYERS);
            return blockState.set(LAYERS, Integer.valueOf(Math.min(8, i + 1)));
        } else {
            return super.getPlacedState(ctx);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LAYERS);
    }
}
