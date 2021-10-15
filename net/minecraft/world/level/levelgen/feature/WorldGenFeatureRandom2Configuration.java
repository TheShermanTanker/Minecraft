package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRandom2;

public class WorldGenFeatureRandom2Configuration extends WorldGenerator<WorldGenFeatureRandom2> {
    public WorldGenFeatureRandom2Configuration(Codec<WorldGenFeatureRandom2> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureRandom2> context) {
        Random random = context.random();
        WorldGenFeatureRandom2 simpleRandomFeatureConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        int i = random.nextInt(simpleRandomFeatureConfiguration.features.size());
        WorldGenFeatureConfigured<?, ?> configuredFeature = simpleRandomFeatureConfiguration.features.get(i).get();
        return configuredFeature.place(worldGenLevel, chunkGenerator, random, blockPos);
    }
}
