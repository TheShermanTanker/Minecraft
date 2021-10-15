package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureLakeConfiguration;

public class WorldGenTaigaStructure extends WorldGenerator<WorldGenFeatureLakeConfiguration> {
    public WorldGenTaigaStructure(Codec<WorldGenFeatureLakeConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureLakeConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();

        WorldGenFeatureLakeConfiguration blockStateConfiguration;
        for(blockStateConfiguration = context.config(); blockPos.getY() > worldGenLevel.getMinBuildHeight() + 3; blockPos = blockPos.below()) {
            if (!worldGenLevel.isEmpty(blockPos.below())) {
                IBlockData blockState = worldGenLevel.getType(blockPos.below());
                if (isDirt(blockState) || isStone(blockState)) {
                    break;
                }
            }
        }

        if (blockPos.getY() <= worldGenLevel.getMinBuildHeight() + 3) {
            return false;
        } else {
            for(int i = 0; i < 3; ++i) {
                int j = random.nextInt(2);
                int k = random.nextInt(2);
                int l = random.nextInt(2);
                float f = (float)(j + k + l) * 0.333F + 0.5F;

                for(BlockPosition blockPos2 : BlockPosition.betweenClosed(blockPos.offset(-j, -k, -l), blockPos.offset(j, k, l))) {
                    if (blockPos2.distSqr(blockPos) <= (double)(f * f)) {
                        worldGenLevel.setTypeAndData(blockPos2, blockStateConfiguration.state, 4);
                    }
                }

                blockPos = blockPos.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
            }

            return true;
        }
    }
}
