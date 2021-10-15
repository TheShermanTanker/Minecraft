package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureOceanRuin;

public class WorldGenFeatureOceanRuinConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureOceanRuinConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureOceanRuin.Temperature.CODEC.fieldOf("biome_temp").forGetter((oceanRuinConfiguration) -> {
            return oceanRuinConfiguration.biomeTemp;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("large_probability").forGetter((oceanRuinConfiguration) -> {
            return oceanRuinConfiguration.largeProbability;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_probability").forGetter((oceanRuinConfiguration) -> {
            return oceanRuinConfiguration.clusterProbability;
        })).apply(instance, WorldGenFeatureOceanRuinConfiguration::new);
    });
    public final WorldGenFeatureOceanRuin.Temperature biomeTemp;
    public final float largeProbability;
    public final float clusterProbability;

    public WorldGenFeatureOceanRuinConfiguration(WorldGenFeatureOceanRuin.Temperature biomeType, float largeProbability, float clusterProbability) {
        this.biomeTemp = biomeType;
        this.largeProbability = largeProbability;
        this.clusterProbability = clusterProbability;
    }
}
