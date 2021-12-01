package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.INamable;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiomeSettingsGeneration {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final BiomeSettingsGeneration EMPTY = new BiomeSettingsGeneration(ImmutableMap.of(), ImmutableList.of());
    public static final MapCodec<BiomeSettingsGeneration> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.simpleMap(WorldGenStage.Features.CODEC, WorldGenCarverWrapper.LIST_CODEC.promotePartial(SystemUtils.prefix("Carver: ", LOGGER::error)).flatXmap(ExtraCodecs.nonNullSupplierListCheck(), ExtraCodecs.nonNullSupplierListCheck()), INamable.keys(WorldGenStage.Features.values())).fieldOf("carvers").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.carvers;
        }), PlacedFeature.LIST_CODEC.promotePartial(SystemUtils.prefix("Feature: ", LOGGER::error)).flatXmap(ExtraCodecs.nonNullSupplierListCheck(), ExtraCodecs.nonNullSupplierListCheck()).listOf().fieldOf("features").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.features;
        })).apply(instance, BiomeSettingsGeneration::new);
    });
    private final Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers;
    private final List<List<Supplier<PlacedFeature>>> features;
    private final List<WorldGenFeatureConfigured<?, ?>> flowerFeatures;
    private final Set<PlacedFeature> featureSet;

    BiomeSettingsGeneration(Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers, List<List<Supplier<PlacedFeature>>> features) {
        this.carvers = carvers;
        this.features = features;
        this.flowerFeatures = features.stream().flatMap(Collection::stream).map(Supplier::get).flatMap(PlacedFeature::getFeatures).filter((configuredFeature) -> {
            return configuredFeature.feature == WorldGenerator.FLOWER;
        }).collect(ImmutableList.toImmutableList());
        this.featureSet = features.stream().flatMap(Collection::stream).map(Supplier::get).collect(Collectors.toSet());
    }

    public List<Supplier<WorldGenCarverWrapper<?>>> getCarvers(WorldGenStage.Features carverStep) {
        return this.carvers.getOrDefault(carverStep, ImmutableList.of());
    }

    public List<WorldGenFeatureConfigured<?, ?>> getFlowerFeatures() {
        return this.flowerFeatures;
    }

    public List<List<Supplier<PlacedFeature>>> features() {
        return this.features;
    }

    public boolean hasFeature(PlacedFeature feature) {
        return this.featureSet.contains(feature);
    }

    public static class Builder {
        private final Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers = Maps.newLinkedHashMap();
        private final List<List<Supplier<PlacedFeature>>> features = Lists.newArrayList();

        public BiomeSettingsGeneration.Builder addFeature(WorldGenStage.Decoration featureStep, PlacedFeature feature) {
            return this.addFeature(featureStep.ordinal(), () -> {
                return feature;
            });
        }

        public BiomeSettingsGeneration.Builder addFeature(int stepIndex, Supplier<PlacedFeature> featureSupplier) {
            this.addFeatureStepsUpTo(stepIndex);
            this.features.get(stepIndex).add(featureSupplier);
            return this;
        }

        public <C extends WorldGenCarverConfiguration> BiomeSettingsGeneration.Builder addCarver(WorldGenStage.Features carverStep, WorldGenCarverWrapper<C> carver) {
            this.carvers.computeIfAbsent(carverStep, (carving) -> {
                return Lists.newArrayList();
            }).add(() -> {
                return carver;
            });
            return this;
        }

        private void addFeatureStepsUpTo(int stepIndex) {
            while(this.features.size() <= stepIndex) {
                this.features.add(Lists.newArrayList());
            }

        }

        public BiomeSettingsGeneration build() {
            return new BiomeSettingsGeneration(this.carvers.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
                return ImmutableList.copyOf(entry.getValue());
            })), this.features.stream().map(ImmutableList::copyOf).collect(ImmutableList.toImmutableList()));
        }
    }
}
