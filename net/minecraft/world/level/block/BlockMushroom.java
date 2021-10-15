package net.minecraft.world.level.block;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockMushroom extends BlockPlant implements IBlockFragilePlantElement {
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    private final Supplier<WorldGenFeatureConfigured<?, ?>> featureSupplier;

    public BlockMushroom(BlockBase.Info settings, Supplier<WorldGenFeatureConfigured<?, ?>> feature) {
        super(settings);
        this.featureSupplier = feature;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (random.nextInt(25) == 0) {
            int i = 5;
            int j = 4;

            for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
                if (world.getType(blockPos).is(this)) {
                    --i;
                    if (i <= 0) {
                        return;
                    }
                }
            }

            BlockPosition blockPos2 = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);

            for(int k = 0; k < 4; ++k) {
                if (world.isEmpty(blockPos2) && state.canPlace(world, blockPos2)) {
                    pos = blockPos2;
                }

                blockPos2 = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            }

            if (world.isEmpty(blockPos2) && state.canPlace(world, blockPos2)) {
                world.setTypeAndData(blockPos2, state, 2);
            }
        }

    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.isSolidRender(world, pos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        if (blockState.is(TagsBlock.MUSHROOM_GROW_BLOCK)) {
            return true;
        } else {
            return world.getLightLevel(pos, 0) < 13 && this.mayPlaceOn(blockState, world, blockPos);
        }
    }

    public boolean growMushroom(WorldServer world, BlockPosition pos, IBlockData state, Random random) {
        world.removeBlock(pos, false);
        if (this.featureSupplier.get().place(world, world.getChunkSource().getChunkGenerator(), random, pos)) {
            return true;
        } else {
            world.setTypeAndData(pos, state, 3);
            return false;
        }
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return (double)random.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.growMushroom(world, pos, state, random);
    }
}
