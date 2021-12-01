package net.minecraft.world.level.levelgen.flat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureFillConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneratorSettingsFlat {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<GeneratorSettingsFlat> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.biomes;
        }), StructureSettings.CODEC.fieldOf("structures").forGetter(GeneratorSettingsFlat::structureSettings), WorldGenFlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(GeneratorSettingsFlat::getLayersInfo), Codec.BOOL.fieldOf("lakes").orElse(false).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.addLakes;
        }), Codec.BOOL.fieldOf("features").orElse(false).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.decoration;
        }), BiomeBase.CODEC.optionalFieldOf("biome").orElseGet(Optional::empty).forGetter((flatLevelGeneratorSettings) -> {
            return Optional.of(flatLevelGeneratorSettings.biome);
        })).apply(instance, GeneratorSettingsFlat::new);
    }).comapFlatMap(GeneratorSettingsFlat::validateHeight, Function.identity()).stable();
    private final IRegistry<BiomeBase> biomes;
    private final StructureSettings structureSettings;
    private final List<WorldGenFlatLayerInfo> layersInfo = Lists.newArrayList();
    private Supplier<BiomeBase> biome;
    private final List<IBlockData> layers;
    private boolean voidGen;
    private boolean decoration;
    private boolean addLakes;

    private static DataResult<GeneratorSettingsFlat> validateHeight(GeneratorSettingsFlat config) {
        int i = config.layersInfo.stream().mapToInt(WorldGenFlatLayerInfo::getHeight).sum();
        return i > DimensionManager.Y_SIZE ? DataResult.error("Sum of layer heights is > " + DimensionManager.Y_SIZE, config) : DataResult.success(config);
    }

    private GeneratorSettingsFlat(IRegistry<BiomeBase> biomeRegistry, StructureSettings structuresConfig, List<WorldGenFlatLayerInfo> layers, boolean hasLakes, boolean hasFeatures, Optional<Supplier<BiomeBase>> biome) {
        this(structuresConfig, biomeRegistry);
        if (hasLakes) {
            this.setAddLakes();
        }

        if (hasFeatures) {
            this.setDecoration();
        }

        this.layersInfo.addAll(layers);
        this.updateLayers();
        if (!biome.isPresent()) {
            LOGGER.error("Unknown biome, defaulting to plains");
            this.biome = () -> {
                return biomeRegistry.getOrThrow(Biomes.PLAINS);
            };
        } else {
            this.biome = biome.get();
        }

    }

    public GeneratorSettingsFlat(StructureSettings structuresConfig, IRegistry<BiomeBase> biomeRegistry) {
        this.biomes = biomeRegistry;
        this.structureSettings = structuresConfig;
        this.biome = () -> {
            return biomeRegistry.getOrThrow(Biomes.PLAINS);
        };
        this.layers = Lists.newArrayList();
    }

    public GeneratorSettingsFlat withStructureSettings(StructureSettings structuresConfig) {
        return this.withLayers(this.layersInfo, structuresConfig);
    }

    public GeneratorSettingsFlat withLayers(List<WorldGenFlatLayerInfo> layers, StructureSettings structuresConfig) {
        GeneratorSettingsFlat flatLevelGeneratorSettings = new GeneratorSettingsFlat(structuresConfig, this.biomes);

        for(WorldGenFlatLayerInfo flatLayerInfo : layers) {
            flatLevelGeneratorSettings.layersInfo.add(new WorldGenFlatLayerInfo(flatLayerInfo.getHeight(), flatLayerInfo.getBlockState().getBlock()));
            flatLevelGeneratorSettings.updateLayers();
        }

        flatLevelGeneratorSettings.setBiome(this.biome);
        if (this.decoration) {
            flatLevelGeneratorSettings.setDecoration();
        }

        if (this.addLakes) {
            flatLevelGeneratorSettings.setAddLakes();
        }

        return flatLevelGeneratorSettings;
    }

    public void setDecoration() {
        this.decoration = true;
    }

    public void setAddLakes() {
        this.addLakes = true;
    }

    public BiomeBase getBiomeFromSettings() {
        BiomeBase biome = this.getBiome();
        BiomeSettingsGeneration biomeGenerationSettings = biome.getGenerationSettings();
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        if (this.addLakes) {
            builder.addFeature(WorldGenStage.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
            builder.addFeature(WorldGenStage.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_SURFACE);
        }

        boolean bl = (!this.voidGen || this.biomes.getResourceKey(biome).equals(Optional.of(Biomes.THE_VOID))) && this.decoration;
        if (bl) {
            List<List<Supplier<PlacedFeature>>> list = biomeGenerationSettings.features();

            for(int i = 0; i < list.size(); ++i) {
                if (i != WorldGenStage.Decoration.UNDERGROUND_STRUCTURES.ordinal() && i != WorldGenStage.Decoration.SURFACE_STRUCTURES.ordinal()) {
                    for(Supplier<PlacedFeature> supplier : list.get(i)) {
                        builder.addFeature(i, supplier);
                    }
                }
            }
        }

        List<IBlockData> list3 = this.getLayers();

        for(int j = 0; j < list3.size(); ++j) {
            IBlockData blockState = list3.get(j);
            if (!HeightMap.Type.MOTION_BLOCKING.isOpaque().test(blockState)) {
                list3.set(j, (IBlockData)null);
                builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, WorldGenerator.FILL_LAYER.configured(new WorldGenFeatureFillConfiguration(j, blockState)).placed());
            }
        }

        return (new BiomeBase.BiomeBuilder()).precipitation(biome.getPrecipitation()).biomeCategory(biome.getBiomeCategory()).temperature(biome.getBaseTemperature()).downfall(biome.getHumidity()).specialEffects(biome.getSpecialEffects()).generationSettings(builder.build()).mobSpawnSettings(biome.getMobSettings()).build();
    }

    public StructureSettings structureSettings() {
        return this.structureSettings;
    }

    public BiomeBase getBiome() {
        return this.biome.get();
    }

    public void setBiome(Supplier<BiomeBase> biome) {
        this.biome = biome;
    }

    public List<WorldGenFlatLayerInfo> getLayersInfo() {
        return this.layersInfo;
    }

    public List<IBlockData> getLayers() {
        return this.layers;
    }

    public void updateLayers() {
        this.layers.clear();

        for(WorldGenFlatLayerInfo flatLayerInfo : this.layersInfo) {
            for(int i = 0; i < flatLayerInfo.getHeight(); ++i) {
                this.layers.add(flatLayerInfo.getBlockState());
            }
        }

        this.voidGen = this.layers.stream().allMatch((state) -> {
            return state.is(Blocks.AIR);
        });
    }

    public static GeneratorSettingsFlat getDefault(IRegistry<BiomeBase> biomeRegistry) {
        StructureSettings structureSettings = new StructureSettings(Optional.of(StructureSettings.DEFAULT_STRONGHOLD), Maps.newHashMap(ImmutableMap.of(StructureGenerator.VILLAGE, StructureSettings.DEFAULTS.get(StructureGenerator.VILLAGE))));
        GeneratorSettingsFlat flatLevelGeneratorSettings = new GeneratorSettingsFlat(structureSettings, biomeRegistry);
        flatLevelGeneratorSettings.biome = () -> {
            return biomeRegistry.getOrThrow(Biomes.PLAINS);
        };
        flatLevelGeneratorSettings.getLayersInfo().add(new WorldGenFlatLayerInfo(1, Blocks.BEDROCK));
        flatLevelGeneratorSettings.getLayersInfo().add(new WorldGenFlatLayerInfo(2, Blocks.DIRT));
        flatLevelGeneratorSettings.getLayersInfo().add(new WorldGenFlatLayerInfo(1, Blocks.GRASS_BLOCK));
        flatLevelGeneratorSettings.updateLayers();
        return flatLevelGeneratorSettings;
    }
}
