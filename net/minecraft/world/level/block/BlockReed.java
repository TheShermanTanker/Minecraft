package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockReed extends Block {
    public static final BlockStateInteger AGE = BlockProperties.AGE_15;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    protected BlockReed(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.isEmpty(pos.above())) {
            int i;
            for(i = 1; world.getType(pos.below(i)).is(this); ++i) {
            }

            if (i < 3) {
                int j = state.get(AGE);
                if (j == 15) {
                    world.setTypeUpdate(pos.above(), this.getBlockData());
                    world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(0)), 4);
                } else {
                    world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(j + 1)), 4);
                }
            }
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!state.canPlace(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        if (blockState.is(this)) {
            return true;
        } else {
            if (blockState.is(TagsBlock.DIRT) || blockState.is(Blocks.SAND) || blockState.is(Blocks.RED_SAND)) {
                BlockPosition blockPos = pos.below();

                for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                    IBlockData blockState2 = world.getType(blockPos.relative(direction));
                    Fluid fluidState = world.getFluid(blockPos.relative(direction));
                    if (fluidState.is(TagsFluid.WATER) || blockState2.is(Blocks.FROSTED_ICE)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }
}
