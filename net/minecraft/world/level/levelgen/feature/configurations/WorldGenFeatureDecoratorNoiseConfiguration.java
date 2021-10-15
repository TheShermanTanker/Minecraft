package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WorldGenFeatureDecoratorNoiseConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenFeatureDecoratorNoiseConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.DOUBLE.fieldOf("noise_level").forGetter((noiseDependantDecoratorConfiguration) -> {
            return noiseDependantDecoratorConfiguration.noiseLevel;
        }), Codec.INT.fieldOf("below_noise").forGetter((noiseDependantDecoratorConfiguration) -> {
            return noiseDependantDecoratorConfiguration.belowNoise;
        }), Codec.INT.fieldOf("above_noise").forGetter((noiseDependantDecoratorConfiguration) -> {
            return noiseDependantDecoratorConfiguration.aboveNoise;
        })).apply(instance, WorldGenFeatureDecoratorNoiseConfiguration::new);
    });
    public final double noiseLevel;
    public final int belowNoise;
    public final int aboveNoise;

    public WorldGenFeatureDecoratorNoiseConfiguration(double noiseLevel, int belowNoise, int aboveNoise) {
        this.noiseLevel = noiseLevel;
        this.belowNoise = belowNoise;
        this.aboveNoise = aboveNoise;
    }
}
