package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class WorldGenFeatureMushroomConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureMushroomConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("cap_provider").forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.capProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("stem_provider").forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.stemProvider;
        }), Codec.INT.fieldOf("foliage_radius").orElse(2).forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.foliageRadius;
        })).apply(instance, WorldGenFeatureMushroomConfiguration::new);
    });
    public final WorldGenFeatureStateProvider capProvider;
    public final WorldGenFeatureStateProvider stemProvider;
    public final int foliageRadius;

    public WorldGenFeatureMushroomConfiguration(WorldGenFeatureStateProvider capProvider, WorldGenFeatureStateProvider stemProvider, int foliageRadius) {
        this.capProvider = capProvider;
        this.stemProvider = stemProvider;
        this.foliageRadius = foliageRadius;
    }
}
