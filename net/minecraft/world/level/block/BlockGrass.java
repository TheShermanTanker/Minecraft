package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFlowers;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;

public class BlockGrass extends BlockDirtSnowSpreadable implements IBlockFragilePlantElement {
    public BlockGrass(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return world.getType(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = Blocks.GRASS.getBlockData();

        label48:
        for(int i = 0; i < 128; ++i) {
            BlockPosition blockPos2 = blockPos;

            for(int j = 0; j < i / 16; ++j) {
                blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!world.getType(blockPos2.below()).is(this) || world.getType(blockPos2).isCollisionShapeFullBlock(world, blockPos2)) {
                    continue label48;
                }
            }

            IBlockData blockState2 = world.getType(blockPos2);
            if (blockState2.is(blockState.getBlock()) && random.nextInt(10) == 0) {
                ((IBlockFragilePlantElement)blockState.getBlock()).performBonemeal(world, random, blockPos2, blockState2);
            }

            if (blockState2.isAir()) {
                IBlockData blockState3;
                if (random.nextInt(8) == 0) {
                    List<WorldGenFeatureConfigured<?, ?>> list = world.getBiome(blockPos2).getGenerationSettings().getFlowerFeatures();
                    if (list.isEmpty()) {
                        continue;
                    }

                    blockState3 = getBlockState(random, blockPos2, list.get(0));
                } else {
                    blockState3 = blockState;
                }

                if (blockState3.canPlace(world, blockPos2)) {
                    world.setTypeAndData(blockPos2, blockState3, 3);
                }
            }
        }

    }

    private static <U extends WorldGenFeatureConfiguration> IBlockData getBlockState(Random random, BlockPosition pos, WorldGenFeatureConfigured<U, ?> flowerFeature) {
        WorldGenFlowers<U> abstractFlowerFeature = (WorldGenFlowers)flowerFeature.feature;
        return abstractFlowerFeature.getRandomFlower(random, pos, flowerFeature.config());
    }
}
