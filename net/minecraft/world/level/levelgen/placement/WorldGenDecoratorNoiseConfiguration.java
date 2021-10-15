package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorNoiseConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenDecoratorNoiseConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("noise_to_count_ratio").forGetter((noiseCountFactorDecoratorConfiguration) -> {
            return noiseCountFactorDecoratorConfiguration.noiseToCountRatio;
        }), Codec.DOUBLE.fieldOf("noise_factor").forGetter((noiseCountFactorDecoratorConfiguration) -> {
            return noiseCountFactorDecoratorConfiguration.noiseFactor;
        }), Codec.DOUBLE.fieldOf("noise_offset").orElse(0.0D).forGetter((noiseCountFactorDecoratorConfiguration) -> {
            return noiseCountFactorDecoratorConfiguration.noiseOffset;
        })).apply(instance, WorldGenDecoratorNoiseConfiguration::new);
    });
    public final int noiseToCountRatio;
    public final double noiseFactor;
    public final double noiseOffset;

    public WorldGenDecoratorNoiseConfiguration(int noiseToCountRatio, double noiseFactor, double noiseOffset) {
        this.noiseToCountRatio = noiseToCountRatio;
        this.noiseFactor = noiseFactor;
        this.noiseOffset = noiseOffset;
    }
}
