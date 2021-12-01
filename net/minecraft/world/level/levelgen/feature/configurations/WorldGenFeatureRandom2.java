package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WorldGenFeatureRandom2 implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRandom2> CODEC = ExtraCodecs.nonEmptyList(PlacedFeature.LIST_CODEC).fieldOf("features").xmap(WorldGenFeatureRandom2::new, (simpleRandomFeatureConfiguration) -> {
        return simpleRandomFeatureConfiguration.features;
    }).codec();
    public final List<Supplier<PlacedFeature>> features;

    public WorldGenFeatureRandom2(List<Supplier<PlacedFeature>> features) {
        this.features = features;
    }

    @Override
    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return this.features.stream().flatMap((supplier) -> {
            return supplier.get().getFeatures();
        });
    }
}
