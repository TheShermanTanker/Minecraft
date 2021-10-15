package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureCompositeConfiguration;
import net.minecraft.world.level.levelgen.placement.WorldGenDecoratorContext;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenFeatureComposite extends WorldGenerator<WorldGenFeatureCompositeConfiguration> {
    public WorldGenFeatureComposite(Codec<WorldGenFeatureCompositeConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureCompositeConfiguration> context) {
        MutableBoolean mutableBoolean = new MutableBoolean();
        GeneratorAccessSeed worldGenLevel = context.level();
        WorldGenFeatureCompositeConfiguration decoratedFeatureConfiguration = context.config();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        WorldGenFeatureConfigured<?, ?> configuredFeature = decoratedFeatureConfiguration.feature.get();
        decoratedFeatureConfiguration.decorator.getPositions(new WorldGenDecoratorContext(worldGenLevel, chunkGenerator), random, blockPos).forEach((blockPosx) -> {
            if (configuredFeature.place(worldGenLevel, chunkGenerator, random, blockPosx)) {
                mutableBoolean.setTrue();
            }

        });
        return mutableBoolean.isTrue();
    }

    @Override
    public String toString() {
        return String.format("< %s [%s] >", this.getClass().getSimpleName(), IRegistry.FEATURE.getKey(this));
    }
}
