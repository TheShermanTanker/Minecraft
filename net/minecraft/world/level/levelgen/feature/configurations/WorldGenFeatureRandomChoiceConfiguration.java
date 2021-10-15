package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureRandomChoiceConfigurationWeight;

public class WorldGenFeatureRandomChoiceConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRandomChoiceConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.apply2(WorldGenFeatureRandomChoiceConfiguration::new, WorldGenFeatureRandomChoiceConfigurationWeight.CODEC.listOf().fieldOf("features").forGetter((randomFeatureConfiguration) -> {
            return randomFeatureConfiguration.features;
        }), WorldGenFeatureConfigured.CODEC.fieldOf("default").forGetter((randomFeatureConfiguration) -> {
            return randomFeatureConfiguration.defaultFeature;
        }));
    });
    public final List<WorldGenFeatureRandomChoiceConfigurationWeight> features;
    public final Supplier<WorldGenFeatureConfigured<?, ?>> defaultFeature;

    public WorldGenFeatureRandomChoiceConfiguration(List<WorldGenFeatureRandomChoiceConfigurationWeight> features, WorldGenFeatureConfigured<?, ?> defaultFeature) {
        this(features, () -> {
            return defaultFeature;
        });
    }

    private WorldGenFeatureRandomChoiceConfiguration(List<WorldGenFeatureRandomChoiceConfigurationWeight> features, Supplier<WorldGenFeatureConfigured<?, ?>> defaultFeature) {
        this.features = features;
        this.defaultFeature = defaultFeature;
    }

    @Override
    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return Stream.concat(this.features.stream().flatMap((weightedConfiguredFeature) -> {
            return weightedConfiguredFeature.feature.get().getFeatures();
        }), this.defaultFeature.get().getFeatures());
    }
}
