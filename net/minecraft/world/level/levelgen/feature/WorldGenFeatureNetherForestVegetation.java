package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.NetherForestVegetationConfig;

public class WorldGenFeatureNetherForestVegetation extends WorldGenerator<NetherForestVegetationConfig> {
    public WorldGenFeatureNetherForestVegetation(Codec<NetherForestVegetationConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<NetherForestVegetationConfig> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        IBlockData blockState = worldGenLevel.getType(blockPos.below());
        NetherForestVegetationConfig netherForestVegetationConfig = context.config();
        Random random = context.random();
        if (!blockState.is(TagsBlock.NYLIUM)) {
            return false;
        } else {
            int i = blockPos.getY();
            if (i >= worldGenLevel.getMinBuildHeight() + 1 && i + 1 < worldGenLevel.getMaxBuildHeight()) {
                int j = 0;

                for(int k = 0; k < netherForestVegetationConfig.spreadWidth * netherForestVegetationConfig.spreadWidth; ++k) {
                    BlockPosition blockPos2 = blockPos.offset(random.nextInt(netherForestVegetationConfig.spreadWidth) - random.nextInt(netherForestVegetationConfig.spreadWidth), random.nextInt(netherForestVegetationConfig.spreadHeight) - random.nextInt(netherForestVegetationConfig.spreadHeight), random.nextInt(netherForestVegetationConfig.spreadWidth) - random.nextInt(netherForestVegetationConfig.spreadWidth));
                    IBlockData blockState2 = netherForestVegetationConfig.stateProvider.getState(random, blockPos2);
                    if (worldGenLevel.isEmpty(blockPos2) && blockPos2.getY() > worldGenLevel.getMinBuildHeight() && blockState2.canPlace(worldGenLevel, blockPos2)) {
                        worldGenLevel.setTypeAndData(blockPos2, blockState2, 2);
                        ++j;
                    }
                }

                return j > 0;
            } else {
                return false;
            }
        }
    }
}
