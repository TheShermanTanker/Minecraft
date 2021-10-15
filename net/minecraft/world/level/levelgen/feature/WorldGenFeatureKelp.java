package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockKelp;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureKelp extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureKelp(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        int i = 0;
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        int j = worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR, blockPos.getX(), blockPos.getZ());
        BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), j, blockPos.getZ());
        if (worldGenLevel.getType(blockPos2).is(Blocks.WATER)) {
            IBlockData blockState = Blocks.KELP.getBlockData();
            IBlockData blockState2 = Blocks.KELP_PLANT.getBlockData();
            int k = 1 + random.nextInt(10);

            for(int l = 0; l <= k; ++l) {
                if (worldGenLevel.getType(blockPos2).is(Blocks.WATER) && worldGenLevel.getType(blockPos2.above()).is(Blocks.WATER) && blockState2.canPlace(worldGenLevel, blockPos2)) {
                    if (l == k) {
                        worldGenLevel.setTypeAndData(blockPos2, blockState.set(BlockKelp.AGE, Integer.valueOf(random.nextInt(4) + 20)), 2);
                        ++i;
                    } else {
                        worldGenLevel.setTypeAndData(blockPos2, blockState2, 2);
                    }
                } else if (l > 0) {
                    BlockPosition blockPos3 = blockPos2.below();
                    if (blockState.canPlace(worldGenLevel, blockPos3) && !worldGenLevel.getType(blockPos3.below()).is(Blocks.KELP)) {
                        worldGenLevel.setTypeAndData(blockPos3, blockState.set(BlockKelp.AGE, Integer.valueOf(random.nextInt(4) + 20)), 2);
                        ++i;
                    }
                    break;
                }

                blockPos2 = blockPos2.above();
            }
        }

        return i > 0;
    }
}
