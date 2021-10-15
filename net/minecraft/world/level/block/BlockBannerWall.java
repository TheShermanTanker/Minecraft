package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBannerWall extends BlockBannerAbstract {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    private static final Map<EnumDirection, VoxelShape> SHAPES = Maps.newEnumMap(ImmutableMap.of(EnumDirection.NORTH, Block.box(0.0D, 0.0D, 14.0D, 16.0D, 12.5D, 16.0D), EnumDirection.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.5D, 2.0D), EnumDirection.WEST, Block.box(14.0D, 0.0D, 0.0D, 16.0D, 12.5D, 16.0D), EnumDirection.EAST, Block.box(0.0D, 0.0D, 0.0D, 2.0D, 12.5D, 16.0D)));

    public BlockBannerWall(EnumColor color, BlockBase.Info settings) {
        super(color, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH));
    }

    @Override
    public String getDescriptionId() {
        return this.getItem().getName();
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.relative(state.get(FACING).opposite())).getMaterial().isBuildable();
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == state.get(FACING).opposite() && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = this.getBlockData();
        IWorldReader levelReader = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection[] directions = ctx.getNearestLookingDirections();

        for(EnumDirection direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                EnumDirection direction2 = direction.opposite();
                blockState = blockState.set(FACING, direction2);
                if (blockState.canPlace(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
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
        builder.add(FACING);
    }
}
