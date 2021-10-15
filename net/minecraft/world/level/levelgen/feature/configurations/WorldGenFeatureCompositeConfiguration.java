package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.placement.WorldGenDecoratorConfigured;

public class WorldGenFeatureCompositeConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureCompositeConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureConfigured.CODEC.fieldOf("feature").forGetter((decoratedFeatureConfiguration) -> {
            return decoratedFeatureConfiguration.feature;
        }), WorldGenDecoratorConfigured.CODEC.fieldOf("decorator").forGetter((decoratedFeatureConfiguration) -> {
            return decoratedFeatureConfiguration.decorator;
        })).apply(instance, WorldGenFeatureCompositeConfiguration::new);
    });
    public final Supplier<WorldGenFeatureConfigured<?, ?>> feature;
    public final WorldGenDecoratorConfigured<?> decorator;

    public WorldGenFeatureCompositeConfiguration(Supplier<WorldGenFeatureConfigured<?, ?>> feature, WorldGenDecoratorConfigured<?> decorator) {
        this.feature = feature;
        this.decorator = decorator;
    }

    @Override
    public String toString() {
        return String.format("< %s [%s | %s] >", this.getClass().getSimpleName(), IRegistry.FEATURE.getKey(this.feature.get().feature()), this.decorator);
    }

    @Override
    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return this.feature.get().getFeatures();
    }
}
