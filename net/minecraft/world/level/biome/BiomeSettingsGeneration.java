package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.SystemUtils;
import net.minecraft.data.worldgen.WorldGenSurfaceComposites;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.INamable;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiomeSettingsGeneration {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final BiomeSettingsGeneration EMPTY = new BiomeSettingsGeneration(() -> {
        return WorldGenSurfaceComposites.NOPE;
    }, ImmutableMap.of(), ImmutableList.of(), ImmutableList.of());
    public static final MapCodec<BiomeSettingsGeneration> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(WorldGenSurfaceComposite.CODEC.fieldOf("surface_builder").flatXmap(ExtraCodecs.nonNullSupplierCheck(), ExtraCodecs.nonNullSupplierCheck()).forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.surfaceBuilder;
        }), Codec.simpleMap(WorldGenStage.Features.CODEC, WorldGenCarverWrapper.LIST_CODEC.promotePartial(SystemUtils.prefix("Carver: ", LOGGER::error)).flatXmap(ExtraCodecs.nonNullSupplierListCheck(), ExtraCodecs.nonNullSupplierListCheck()), INamable.keys(WorldGenStage.Features.values())).fieldOf("carvers").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.carvers;
        }), WorldGenFeatureConfigured.LIST_CODEC.promotePartial(SystemUtils.prefix("Feature: ", LOGGER::error)).flatXmap(ExtraCodecs.nonNullSupplierListCheck(), ExtraCodecs.nonNullSupplierListCheck()).listOf().fieldOf("features").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.features;
        }), StructureFeature.LIST_CODEC.promotePartial(SystemUtils.prefix("Structure start: ", LOGGER::error)).fieldOf("starts").flatXmap(ExtraCodecs.nonNullSupplierListCheck(), ExtraCodecs.nonNullSupplierListCheck()).forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.structureStarts;
        })).apply(instance, BiomeSettingsGeneration::new);
    });
    private final Supplier<WorldGenSurfaceComposite<?>> surfaceBuilder;
    private final Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers;
    private final List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> features;
    private final List<Supplier<StructureFeature<?, ?>>> structureStarts;
    private final List<WorldGenFeatureConfigured<?, ?>> flowerFeatures;

    BiomeSettingsGeneration(Supplier<WorldGenSurfaceComposite<?>> surfaceBuilder, Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers, List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> features, List<Supplier<StructureFeature<?, ?>>> structureFeatures) {
        this.surfaceBuilder = surfaceBuilder;
        this.carvers = carvers;
        this.features = features;
        this.structureStarts = structureFeatures;
        this.flowerFeatures = features.stream().flatMap(Collection::stream).map(Supplier::get).flatMap(WorldGenFeatureConfigured::getFeatures).filter((configuredFeature) -> {
            return configuredFeature.feature == WorldGenerator.FLOWER;
        }).collect(ImmutableList.toImmutableList());
    }

    public List<Supplier<WorldGenCarverWrapper<?>>> getCarvers(WorldGenStage.Features carverStep) {
        return this.carvers.getOrDefault(carverStep, ImmutableList.of());
    }

    public boolean isValidStart(StructureGenerator<?> structureFeature) {
        return this.structureStarts.stream().anyMatch((supplier) -> {
            return (supplier.get()).feature == structureFeature;
        });
    }

    public Collection<Supplier<StructureFeature<?, ?>>> structures() {
        return this.structureStarts;
    }

    public StructureFeature<?, ?> withBiomeConfig(StructureFeature<?, ?> configuredStructureFeature) {
        return DataFixUtils.orElse(this.structureStarts.stream().map(Supplier::get).filter((configuredStructureFeature2) -> {
            return configuredStructureFeature2.feature == configuredStructureFeature.feature;
        }).findAny(), configuredStructureFeature);
    }

    public List<WorldGenFeatureConfigured<?, ?>> getFlowerFeatures() {
        return this.flowerFeatures;
    }

    public List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> features() {
        return this.features;
    }

    public Supplier<WorldGenSurfaceComposite<?>> getSurfaceBuilder() {
        return this.surfaceBuilder;
    }

    public WorldGenSurfaceConfiguration getSurfaceBuilderConfig() {
        return this.surfaceBuilder.get().config();
    }

    public static class Builder {
        private Optional<Supplier<WorldGenSurfaceComposite<?>>> surfaceBuilder = Optional.empty();
        private final Map<WorldGenStage.Features, List<Supplier<WorldGenCarverWrapper<?>>>> carvers = Maps.newLinkedHashMap();
        private final List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> features = Lists.newArrayList();
        private final List<Supplier<StructureFeature<?, ?>>> structureStarts = Lists.newArrayList();

        public BiomeSettingsGeneration.Builder surfaceBuilder(WorldGenSurfaceComposite<?> surfaceBuilder) {
            return this.surfaceBuilder(() -> {
                return surfaceBuilder;
            });
        }

        public BiomeSettingsGeneration.Builder surfaceBuilder(Supplier<WorldGenSurfaceComposite<?>> surfaceBuilderSupplier) {
            this.surfaceBuilder = Optional.of(surfaceBuilderSupplier);
            return this;
        }

        public BiomeSettingsGeneration.Builder addFeature(WorldGenStage.Decoration featureStep, WorldGenFeatureConfigured<?, ?> feature) {
            return this.addFeature(featureStep.ordinal(), () -> {
                return feature;
            });
        }

        public BiomeSettingsGeneration.Builder addFeature(int stepIndex, Supplier<WorldGenFeatureConfigured<?, ?>> featureSupplier) {
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

        public BiomeSettingsGeneration.Builder addStructureStart(StructureFeature<?, ?> structureFeature) {
            this.structureStarts.add(() -> {
                return structureFeature;
            });
            return this;
        }

        private void addFeatureStepsUpTo(int stepIndex) {
            while(this.features.size() <= stepIndex) {
                this.features.add(Lists.newArrayList());
            }

        }

        public BiomeSettingsGeneration build() {
            return new BiomeSettingsGeneration(this.surfaceBuilder.orElseThrow(() -> {
                return new IllegalStateException("Missing surface builder");
            }), this.carvers.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
                return ImmutableList.copyOf(entry.getValue());
            })), this.features.stream().map(ImmutableList::copyOf).collect(ImmutableList.toImmutableList()), ImmutableList.copyOf(this.structureStarts));
        }
    }
}
