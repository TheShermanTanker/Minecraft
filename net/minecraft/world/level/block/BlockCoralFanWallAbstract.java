package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCoralFanWallAbstract extends BlockCoralFanAbstract {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    private static final Map<EnumDirection, VoxelShape> SHAPES = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, Block.box(0.0D, 4.0D, 5.0D, 16.0D, 12.0D, 16.0D), EnumDirection.SOUTH, Block.box(0.0D, 4.0D, 0.0D, 16.0D, 12.0D, 11.0D), EnumDirection.WEST, Block.box(5.0D, 4.0D, 0.0D, 16.0D, 12.0D, 16.0D), EnumDirection.EAST, Block.box(0.0D, 4.0D, 0.0D, 11.0D, 12.0D, 16.0D)));

    protected BlockCoralFanWallAbstract(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(WATERLOGGED, Boolean.valueOf(true)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return direction.opposite() == state.get(FACING) && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : state;
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction.opposite());
        IBlockData blockState = world.getType(blockPos);
        return blockState.isFaceSturdy(world, blockPos, direction);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = super.getPlacedState(ctx);
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection[] directions = ctx.getNearestLookingDirections();

        for(EnumDirection direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                blockState = blockState.set(FACING, direction.opposite());
                if (blockState.canPlace(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
    }
}
