package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChoiceConfiguration;

public class WorldGenFeatureChoice extends WorldGenerator<WorldGenFeatureChoiceConfiguration> {
    public WorldGenFeatureChoice(Codec<WorldGenFeatureChoiceConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureChoiceConfiguration> context) {
        Random random = context.random();
        WorldGenFeatureChoiceConfiguration randomBooleanFeatureConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPosition blockPos = context.origin();
        boolean bl = random.nextBoolean();
        return bl ? randomBooleanFeatureConfiguration.featureTrue.get().place(worldGenLevel, chunkGenerator, random, blockPos) : randomBooleanFeatureConfiguration.featureFalse.get().place(worldGenLevel, chunkGenerator, random, blockPos);
    }
}
