package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCactus extends Block {
    public static final BlockStateInteger AGE = BlockProperties.AGE_15;
    public static final int MAX_AGE = 15;
    protected static final int AABB_OFFSET = 1;
    protected static final VoxelShape COLLISION_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);
    protected static final VoxelShape OUTLINE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    protected BlockCactus(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!state.canPlace(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos.above();
        if (world.isEmpty(blockPos)) {
            int i;
            for(i = 1; world.getType(pos.below(i)).is(this); ++i) {
            }

            if (i < 3) {
                int j = state.get(AGE);
                if (j == 15) {
                    world.setTypeUpdate(blockPos, this.getBlockData());
                    IBlockData blockState = state.set(AGE, Integer.valueOf(0));
                    world.setTypeAndData(pos, blockState, 4);
                    blockState.doPhysics(world, blockPos, this, pos, false);
                } else {
                    world.setTypeAndData(pos, state.set(AGE, Integer.valueOf(j + 1)), 4);
                }

            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return OUTLINE_SHAPE;
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
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            IBlockData blockState = world.getType(pos.relative(direction));
            Material material = blockState.getMaterial();
            if (material.isBuildable() || world.getFluid(pos.relative(direction)).is(TagsFluid.LAVA)) {
                return false;
            }
        }

        IBlockData blockState2 = world.getType(pos.below());
        return (blockState2.is(Blocks.CACTUS) || blockState2.is(Blocks.SAND) || blockState2.is(Blocks.RED_SAND)) && !world.getType(pos.above()).getMaterial().isLiquid();
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        entity.damageEntity(DamageSource.CACTUS, 1.0F);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
