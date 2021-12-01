package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDripleafStemBig extends BlockFacingHorizontal implements IBlockFragilePlantElement, IBlockWaterlogged {
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final int STEM_WIDTH = 6;
    protected static final VoxelShape NORTH_SHAPE = Block.box(5.0D, 0.0D, 9.0D, 11.0D, 16.0D, 15.0D);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(5.0D, 0.0D, 1.0D, 11.0D, 16.0D, 7.0D);
    protected static final VoxelShape EAST_SHAPE = Block.box(1.0D, 0.0D, 5.0D, 7.0D, 16.0D, 11.0D);
    protected static final VoxelShape WEST_SHAPE = Block.box(9.0D, 0.0D, 5.0D, 15.0D, 16.0D, 11.0D);

    protected BlockDripleafStemBig(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)).set(FACING, EnumDirection.NORTH));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection)state.get(FACING)) {
        case SOUTH:
            return SOUTH_SHAPE;
        case NORTH:
        default:
            return NORTH_SHAPE;
        case WEST:
            return WEST_SHAPE;
        case EAST:
            return EAST_SHAPE;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        IBlockData blockState2 = world.getType(pos.above());
        return (blockState.is(this) || blockState.is(TagsBlock.BIG_DRIPLEAF_PLACEABLE)) && (blockState2.is(this) || blockState2.is(Blocks.BIG_DRIPLEAF));
    }

    protected static boolean place(GeneratorAccess world, BlockPosition pos, Fluid fluidState, EnumDirection direction) {
        IBlockData blockState = Blocks.BIG_DRIPLEAF_STEM.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.isSourceOfType(FluidTypes.WATER))).set(FACING, direction);
        return world.setTypeAndData(pos, blockState, 3);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if ((direction == EnumDirection.DOWN || direction == EnumDirection.UP) && !state.canPlace(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        Optional<BlockPosition> optional = BlockUtil.getTopConnectedBlock(world, pos, state.getBlock(), EnumDirection.UP, Blocks.BIG_DRIPLEAF);
        if (!optional.isPresent()) {
            return false;
        } else {
            BlockPosition blockPos = optional.get().above();
            IBlockData blockState = world.getType(blockPos);
            return BlockDripleafBig.canPlaceAt(world, blockPos, blockState);
        }
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        Optional<BlockPosition> optional = BlockUtil.getTopConnectedBlock(world, pos, state.getBlock(), EnumDirection.UP, Blocks.BIG_DRIPLEAF);
        if (optional.isPresent()) {
            BlockPosition blockPos = optional.get();
            BlockPosition blockPos2 = blockPos.above();
            EnumDirection direction = state.get(FACING);
            place(world, blockPos, world.getFluid(blockPos), direction);
            BlockDripleafBig.place(world, blockPos2, world.getFluid(blockPos2), direction);
        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Blocks.BIG_DRIPLEAF);
    }
}
