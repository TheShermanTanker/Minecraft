package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WorldGenFeatureRandomChoiceConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRandomChoiceConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.apply2(WorldGenFeatureRandomChoiceConfiguration::new, WeightedPlacedFeature.CODEC.listOf().fieldOf("features").forGetter((randomFeatureConfiguration) -> {
            return randomFeatureConfiguration.features;
        }), PlacedFeature.CODEC.fieldOf("default").forGetter((randomFeatureConfiguration) -> {
            return randomFeatureConfiguration.defaultFeature;
        }));
    });
    public final List<WeightedPlacedFeature> features;
    public final Supplier<PlacedFeature> defaultFeature;

    public WorldGenFeatureRandomChoiceConfiguration(List<WeightedPlacedFeature> features, PlacedFeature defaultFeature) {
        this(features, () -> {
            return defaultFeature;
        });
    }

    private WorldGenFeatureRandomChoiceConfiguration(List<WeightedPlacedFeature> features, Supplier<PlacedFeature> defaultFeature) {
        this.features = features;
        this.defaultFeature = defaultFeature;
    }

    @Override
    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return Stream.concat(this.features.stream().flatMap((weightedPlacedFeature) -> {
            return weightedPlacedFeature.feature.get().getFeatures();
        }), this.defaultFeature.get().getFeatures());
    }
}
