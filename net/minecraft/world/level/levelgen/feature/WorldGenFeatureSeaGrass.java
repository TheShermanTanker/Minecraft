package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;

public class WorldGenFeatureSeaGrass extends WorldGenerator<WorldGenFeatureConfigurationChance> {
    public WorldGenFeatureSeaGrass(Codec<WorldGenFeatureConfigurationChance> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureConfigurationChance> context) {
        boolean bl = false;
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        WorldGenFeatureConfigurationChance probabilityFeatureConfiguration = context.config();
        int i = random.nextInt(8) - random.nextInt(8);
        int j = random.nextInt(8) - random.nextInt(8);
        int k = worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR, blockPos.getX() + i, blockPos.getZ() + j);
        BlockPosition blockPos2 = new BlockPosition(blockPos.getX() + i, k, blockPos.getZ() + j);
        if (worldGenLevel.getType(blockPos2).is(Blocks.WATER)) {
            boolean bl2 = random.nextDouble() < (double)probabilityFeatureConfiguration.probability;
            IBlockData blockState = bl2 ? Blocks.TALL_SEAGRASS.getBlockData() : Blocks.SEAGRASS.getBlockData();
            if (blockState.canPlace(worldGenLevel, blockPos2)) {
                if (bl2) {
                    IBlockData blockState2 = blockState.set(TallSeagrassBlock.HALF, BlockPropertyDoubleBlockHalf.UPPER);
                    BlockPosition blockPos3 = blockPos2.above();
                    if (worldGenLevel.getType(blockPos3).is(Blocks.WATER)) {
                        worldGenLevel.setTypeAndData(blockPos2, blockState, 2);
                        worldGenLevel.setTypeAndData(blockPos3, blockState2, 2);
                    }
                } else {
                    worldGenLevel.setTypeAndData(blockPos2, blockState, 2);
                }

                bl = true;
            }
        }

        return bl;
    }
}
