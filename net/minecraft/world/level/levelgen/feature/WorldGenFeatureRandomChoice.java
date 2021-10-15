package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRandomChoiceConfiguration;

public class WorldGenFeatureRandomChoice extends WorldGenerator<WorldGenFeatureRandomChoiceConfiguration> {
    public WorldGenFeatureRandomChoice(Codec<WorldGenFeatureRandomChoiceConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureRandomChoiceConfiguration> context) {
        WorldGenFeatureRandomChoiceConfiguration randomFeatureConfiguration = context.config();
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPosition blockPos = context.origin();

        for(WorldGenFeatureRandomChoiceConfigurationWeight weightedConfiguredFeature : randomFeatureConfiguration.features) {
            if (random.nextFloat() < weightedConfiguredFeature.chance) {
                return weightedConfiguredFeature.place(worldGenLevel, chunkGenerator, random, blockPos);
            }
        }

        return randomFeatureConfiguration.defaultFeature.get().place(worldGenLevel, chunkGenerator, random, blockPos);
    }
}
