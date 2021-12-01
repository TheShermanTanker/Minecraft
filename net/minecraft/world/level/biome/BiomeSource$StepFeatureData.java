package net.minecraft.world.level.biome;

import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record BiomeSource$StepFeatureData(List<PlacedFeature> features, ToIntFunction<PlacedFeature> indexMapping) {
    public BiomeSource$StepFeatureData(List<PlacedFeature> list, ToIntFunction<PlacedFeature> toIntFunction) {
        this.features = list;
        this.indexMapping = toIntFunction;
    }

    public List<PlacedFeature> features() {
        return this.features;
    }

    public ToIntFunction<PlacedFeature> indexMapping() {
        return this.indexMapping;
    }
}
