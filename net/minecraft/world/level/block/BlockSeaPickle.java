package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
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
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSeaPickle extends BlockPlant implements IBlockFragilePlantElement, IBlockWaterlogged {
    public static final int MAX_PICKLES = 4;
    public static final BlockStateInteger PICKLES = BlockProperties.PICKLES;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape ONE_AABB = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);
    protected static final VoxelShape TWO_AABB = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D);
    protected static final VoxelShape THREE_AABB = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);
    protected static final VoxelShape FOUR_AABB = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D);

    protected BlockSeaPickle(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(PICKLES, Integer.valueOf(1)).set(WATERLOGGED, Boolean.valueOf(true)));
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition());
        if (blockState.is(this)) {
            return blockState.set(PICKLES, Integer.valueOf(Math.min(4, blockState.get(PICKLES) + 1)));
        } else {
            Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
            boolean bl = fluidState.getType() == FluidTypes.WATER;
            return super.getPlacedState(ctx).set(WATERLOGGED, Boolean.valueOf(bl));
        }
    }

    public static boolean isDead(IBlockData state) {
        return !state.get(WATERLOGGED);
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return !floor.getCollisionShape(world, pos).getFaceShape(EnumDirection.UP).isEmpty() || floor.isFaceSturdy(world, pos, EnumDirection.UP);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return this.mayPlaceOn(world.getType(blockPos), world, blockPos);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!state.canPlace(world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            if (state.get(WATERLOGGED)) {
                world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return !context.isSneaking() && context.getItemStack().is(this.getItem()) && state.get(PICKLES) < 4 ? true : super.canBeReplaced(state, context);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch(state.get(PICKLES)) {
        case 1:
        default:
            return ONE_AABB;
        case 2:
            return TWO_AABB;
        case 3:
            return THREE_AABB;
        case 4:
            return FOUR_AABB;
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(PICKLES, WATERLOGGED);
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
        if (!isDead(state) && world.getType(pos.below()).is(TagsBlock.CORAL_BLOCKS)) {
            int i = 5;
            int j = 1;
            int k = 2;
            int l = 0;
            int m = pos.getX() - 2;
            int n = 0;

            for(int o = 0; o < 5; ++o) {
                for(int p = 0; p < j; ++p) {
                    int q = 2 + pos.getY() - 1;

                    for(int r = q - 2; r < q; ++r) {
                        BlockPosition blockPos = new BlockPosition(m + o, r, pos.getZ() - n + p);
                        if (blockPos != pos && random.nextInt(6) == 0 && world.getType(blockPos).is(Blocks.WATER)) {
                            IBlockData blockState = world.getType(blockPos.below());
                            if (blockState.is(TagsBlock.CORAL_BLOCKS)) {
                                world.setTypeAndData(blockPos, Blocks.SEA_PICKLE.getBlockData().set(PICKLES, Integer.valueOf(random.nextInt(4) + 1)), 3);
                            }
                        }
                    }
                }

                if (l < 2) {
                    j += 2;
                    ++n;
                } else {
                    j -= 2;
                    --n;
                }

                ++l;
            }

            world.setTypeAndData(pos, state.set(PICKLES, Integer.valueOf(4)), 2);
        }

    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
