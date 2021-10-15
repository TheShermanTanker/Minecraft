package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDripleafSmall extends BlockTallPlant implements IBlockFragilePlantElement, IBlockWaterlogged {
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateDirection FACING = BlockProperties.HORIZONTAL_FACING;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public BlockDripleafSmall(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HALF, BlockPropertyDoubleBlockHalf.LOWER).set(WATERLOGGED, Boolean.valueOf(false)).set(FACING, EnumDirection.NORTH));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(TagsBlock.SMALL_DRIPLEAF_PLACEABLE) || world.getFluid(pos.above()).isSourceOfType(FluidTypes.WATER) && super.mayPlaceOn(floor, world, pos);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = super.getPlacedState(ctx);
        return blockState != null ? copyWaterloggedFrom(ctx.getWorld(), ctx.getClickPosition(), blockState.set(FACING, ctx.getHorizontalDirection().opposite())) : null;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (!world.isClientSide()) {
            BlockPosition blockPos = pos.above();
            IBlockData blockState = BlockTallPlant.copyWaterloggedFrom(world, blockPos, this.getBlockData().set(HALF, BlockPropertyDoubleBlockHalf.UPPER).set(FACING, state.get(FACING)));
            world.setTypeAndData(blockPos, blockState, 3);
        }

    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        if (state.get(HALF) == BlockPropertyDoubleBlockHalf.UPPER) {
            return super.canPlace(state, world, pos);
        } else {
            BlockPosition blockPos = pos.below();
            IBlockData blockState = world.getType(blockPos);
            return this.mayPlaceOn(blockState, world, blockPos);
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HALF, WATERLOGGED, FACING);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        if (state.get(BlockTallPlant.HALF) == BlockPropertyDoubleBlockHalf.LOWER) {
            BlockPosition blockPos = pos.above();
            world.setTypeAndData(blockPos, world.getFluid(blockPos).getBlockData(), 18);
            BlockDripleafBig.placeWithRandomHeight(world, random, pos, state.get(FACING));
        } else {
            BlockPosition blockPos2 = pos.below();
            this.performBonemeal(world, random, blockPos2, world.getType(blockPos2));
        }

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
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XYZ;
    }

    @Override
    public float getMaxVerticalOffset() {
        return 0.1F;
    }
}
