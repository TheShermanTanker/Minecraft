package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public record LakeFeature$Configuration(WorldGenFeatureStateProvider fluid, WorldGenFeatureStateProvider barrier) implements WorldGenFeatureConfiguration {
    public static final Codec<LakeFeature$Configuration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("fluid").forGetter(LakeFeature$Configuration::fluid), WorldGenFeatureStateProvider.CODEC.fieldOf("barrier").forGetter(LakeFeature$Configuration::barrier)).apply(instance, LakeFeature$Configuration::new);
    });

    public LakeFeature$Configuration(WorldGenFeatureStateProvider blockStateProvider, WorldGenFeatureStateProvider blockStateProvider2) {
        this.fluid = blockStateProvider;
        this.barrier = blockStateProvider2;
    }

    public WorldGenFeatureStateProvider fluid() {
        return this.fluid;
    }

    public WorldGenFeatureStateProvider barrier() {
        return this.barrier;
    }
}
