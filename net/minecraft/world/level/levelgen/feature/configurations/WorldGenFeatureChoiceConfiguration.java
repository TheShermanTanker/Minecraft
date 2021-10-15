package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public class WorldGenFeatureChoiceConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureChoiceConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureConfigured.CODEC.fieldOf("feature_true").forGetter((randomBooleanFeatureConfiguration) -> {
            return randomBooleanFeatureConfiguration.featureTrue;
        }), WorldGenFeatureConfigured.CODEC.fieldOf("feature_false").forGetter((randomBooleanFeatureConfiguration) -> {
            return randomBooleanFeatureConfiguration.featureFalse;
        })).apply(instance, WorldGenFeatureChoiceConfiguration::new);
    });
    public final Supplier<WorldGenFeatureConfigured<?, ?>> featureTrue;
    public final Supplier<WorldGenFeatureConfigured<?, ?>> featureFalse;

    public WorldGenFeatureChoiceConfiguration(Supplier<WorldGenFeatureConfigured<?, ?>> featureTrue, Supplier<WorldGenFeatureConfigured<?, ?>> featureFalse) {
        this.featureTrue = featureTrue;
        this.featureFalse = featureFalse;
    }

    @Override
    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return Stream.concat(this.featureTrue.get().getFeatures(), this.featureFalse.get().getFeatures());
    }
}
