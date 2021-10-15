package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WorldGenFeatureConfigurationChance implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureConfigurationChance> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((probabilityFeatureConfiguration) -> {
            return probabilityFeatureConfiguration.probability;
        })).apply(instance, WorldGenFeatureConfigurationChance::new);
    });
    public final float probability;

    public WorldGenFeatureConfigurationChance(float probability) {
        this.probability = probability;
    }
}
