package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BiomeBase {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<BiomeBase> DIRECT_CODEC;
    public static final Codec<BiomeBase> NETWORK_CODEC;
    public static final Codec<Supplier<BiomeBase>> CODEC = RegistryFileCodec.create(IRegistry.BIOME_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<Supplier<BiomeBase>>> LIST_CODEC = RegistryFileCodec.homogeneousList(IRegistry.BIOME_REGISTRY, DIRECT_CODEC);
    private final Map<Integer, List<StructureGenerator<?>>> structuresByStep = IRegistry.STRUCTURE_FEATURE.stream().collect(Collectors.groupingBy((structureFeature) -> {
        return structureFeature.step().ordinal();
    }));
    private static final NoiseGenerator3 TEMPERATURE_NOISE = new NoiseGenerator3(new SeededRandom(1234L), ImmutableList.of(0));
    static final NoiseGenerator3 FROZEN_TEMPERATURE_NOISE = new NoiseGenerator3(new SeededRandom(3456L), ImmutableList.of(-2, -1, 0));
    public static final NoiseGenerator3 BIOME_INFO_NOISE = new NoiseGenerator3(new SeededRandom(2345L), ImmutableList.of(0));
    private static final int TEMPERATURE_CACHE_SIZE = 1024;
    private final BiomeBase.ClimateSettings climateSettings;
    private final BiomeSettingsGeneration generationSettings;
    private final BiomeSettingsMobs mobSettings;
    private final float depth;
    private final float scale;
    private final BiomeBase.Geography biomeCategory;
    private final BiomeFog specialEffects;
    private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
        return SystemUtils.make(() -> {
            Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
                @Override
                protected void rehash(int i) {
                }
            };
            long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
            return long2FloatLinkedOpenHashMap;
        });
    });

    BiomeBase(BiomeBase.ClimateSettings weather, BiomeBase.Geography category, float depth, float scale, BiomeFog effects, BiomeSettingsGeneration generationSettings, BiomeSettingsMobs spawnSettings) {
        this.climateSettings = weather;
        this.generationSettings = generationSettings;
        this.mobSettings = spawnSettings;
        this.biomeCategory = category;
        this.depth = depth;
        this.scale = scale;
        this.specialEffects = effects;
    }

    public int getSkyColor() {
        return this.specialEffects.getSkyColor();
    }

    public BiomeSettingsMobs getMobSettings() {
        return this.mobSettings;
    }

    public BiomeBase.Precipitation getPrecipitation() {
        return this.climateSettings.precipitation;
    }

    public boolean isHumid() {
        return this.getHumidity() > 0.85F;
    }

    private float getHeightAdjustedTemperature(BlockPosition pos) {
        float f = this.climateSettings.temperatureModifier.modifyTemperature(pos, this.getBaseTemperature());
        if (pos.getY() > 64) {
            float g = (float)(TEMPERATURE_NOISE.getValue((double)((float)pos.getX() / 8.0F), (double)((float)pos.getZ() / 8.0F), false) * 4.0D);
            return f - (g + (float)pos.getY() - 64.0F) * 0.05F / 30.0F;
        } else {
            return f;
        }
    }

    public final float getAdjustedTemperature(BlockPosition blockPos) {
        long l = blockPos.asLong();
        Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = this.temperatureCache.get();
        float f = long2FloatLinkedOpenHashMap.get(l);
        if (!Float.isNaN(f)) {
            return f;
        } else {
            float g = this.getHeightAdjustedTemperature(blockPos);
            if (long2FloatLinkedOpenHashMap.size() == 1024) {
                long2FloatLinkedOpenHashMap.removeFirstFloat();
            }

            long2FloatLinkedOpenHashMap.put(l, g);
            return g;
        }
    }

    public boolean shouldFreeze(IWorldReader world, BlockPosition blockPos) {
        return this.shouldFreeze(world, blockPos, true);
    }

    public boolean shouldFreeze(IWorldReader world, BlockPosition pos, boolean doWaterCheck) {
        if (this.getAdjustedTemperature(pos) >= 0.15F) {
            return false;
        } else {
            if (pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && world.getBrightness(EnumSkyBlock.BLOCK, pos) < 10) {
                IBlockData blockState = world.getType(pos);
                Fluid fluidState = world.getFluid(pos);
                if (fluidState.getType() == FluidTypes.WATER && blockState.getBlock() instanceof BlockFluids) {
                    if (!doWaterCheck) {
                        return true;
                    }

                    boolean bl = world.isWaterAt(pos.west()) && world.isWaterAt(pos.east()) && world.isWaterAt(pos.north()) && world.isWaterAt(pos.south());
                    if (!bl) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public boolean isColdEnoughToSnow(BlockPosition pos) {
        return this.getAdjustedTemperature(pos) < 0.15F;
    }

    public boolean shouldSnow(IWorldReader world, BlockPosition blockPos) {
        if (!this.isColdEnoughToSnow(blockPos)) {
            return false;
        } else {
            if (blockPos.getY() >= world.getMinBuildHeight() && blockPos.getY() < world.getMaxBuildHeight() && world.getBrightness(EnumSkyBlock.BLOCK, blockPos) < 10) {
                IBlockData blockState = world.getType(blockPos);
                if (blockState.isAir() && Blocks.SNOW.getBlockData().canPlace(world, blockPos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public BiomeSettingsGeneration getGenerationSettings() {
        return this.generationSettings;
    }

    public void generate(StructureManager structureAccessor, ChunkGenerator chunkGenerator, RegionLimitedWorldAccess region, long populationSeed, SeededRandom random, BlockPosition origin) {
        List<List<Supplier<WorldGenFeatureConfigured<?, ?>>>> list = this.generationSettings.features();
        IRegistry<WorldGenFeatureConfigured<?, ?>> registry = region.registryAccess().registryOrThrow(IRegistry.CONFIGURED_FEATURE_REGISTRY);
        IRegistry<StructureGenerator<?>> registry2 = region.registryAccess().registryOrThrow(IRegistry.STRUCTURE_FEATURE_REGISTRY);
        int i = WorldGenStage.Decoration.values().length;

        for(int j = 0; j < i; ++j) {
            int k = 0;
            if (structureAccessor.shouldGenerateFeatures()) {
                for(StructureGenerator<?> structureFeature : this.structuresByStep.getOrDefault(j, Collections.emptyList())) {
                    random.setFeatureSeed(populationSeed, k, j);
                    int l = SectionPosition.blockToSectionCoord(origin.getX());
                    int m = SectionPosition.blockToSectionCoord(origin.getZ());
                    int n = SectionPosition.sectionToBlockCoord(l);
                    int o = SectionPosition.sectionToBlockCoord(m);
                    Supplier<String> supplier = () -> {
                        return registry2.getResourceKey(structureFeature).map(Object::toString).orElseGet(structureFeature::toString);
                    };

                    try {
                        int p = region.getMinBuildHeight() + 1;
                        int q = region.getMaxBuildHeight() - 1;
                        region.setCurrentlyGenerating(supplier);
                        structureAccessor.startsForFeature(SectionPosition.of(origin), structureFeature).forEach((structureStart) -> {
                            structureStart.placeInChunk(region, structureAccessor, chunkGenerator, random, new StructureBoundingBox(n, p, o, n + 15, q, o + 15), new ChunkCoordIntPair(l, m));
                        });
                    } catch (Exception var24) {
                        CrashReport crashReport = CrashReport.forThrowable(var24, "Feature placement");
                        crashReport.addCategory("Feature").setDetail("Description", supplier::get);
                        throw new ReportedException(crashReport);
                    }

                    ++k;
                }
            }

            if (list.size() > j) {
                for(Supplier<WorldGenFeatureConfigured<?, ?>> supplier2 : list.get(j)) {
                    WorldGenFeatureConfigured<?, ?> configuredFeature = supplier2.get();
                    Supplier<String> supplier3 = () -> {
                        return registry.getResourceKey(configuredFeature).map(Object::toString).orElseGet(configuredFeature::toString);
                    };
                    random.setFeatureSeed(populationSeed, k, j);

                    try {
                        region.setCurrentlyGenerating(supplier3);
                        configuredFeature.place(region, chunkGenerator, random, origin);
                    } catch (Exception var25) {
                        CrashReport crashReport2 = CrashReport.forThrowable(var25, "Feature placement");
                        crashReport2.addCategory("Feature").setDetail("Description", supplier3::get);
                        throw new ReportedException(crashReport2);
                    }

                    ++k;
                }
            }
        }

        region.setCurrentlyGenerating((Supplier<String>)null);
    }

    public int getFogColor() {
        return this.specialEffects.getFogColor();
    }

    public int getGrassColor(double x, double z) {
        int i = this.specialEffects.getGrassColorOverride().orElseGet(this::getGrassColorFromTexture);
        return this.specialEffects.getGrassColorModifier().modifyColor(x, z, i);
    }

    private int getGrassColorFromTexture() {
        double d = (double)MathHelper.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double e = (double)MathHelper.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return GrassColor.get(d, e);
    }

    public int getFoliageColor() {
        return this.specialEffects.getFoliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
    }

    private int getFoliageColorFromTexture() {
        double d = (double)MathHelper.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double e = (double)MathHelper.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return FoliageColor.get(d, e);
    }

    public void buildSurfaceAt(Random random, IChunkAccess chunk, int x, int z, int worldHeight, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l) {
        WorldGenSurfaceComposite<?> configuredSurfaceBuilder = this.generationSettings.getSurfaceBuilder().get();
        configuredSurfaceBuilder.initNoise(l);
        configuredSurfaceBuilder.apply(random, chunk, this, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, i, l);
    }

    public final float getDepth() {
        return this.depth;
    }

    public final float getHumidity() {
        return this.climateSettings.downfall;
    }

    public final float getScale() {
        return this.scale;
    }

    public final float getBaseTemperature() {
        return this.climateSettings.temperature;
    }

    public BiomeFog getSpecialEffects() {
        return this.specialEffects;
    }

    public final int getWaterColor() {
        return this.specialEffects.getWaterColor();
    }

    public final int getWaterFogColor() {
        return this.specialEffects.getWaterFogColor();
    }

    public Optional<BiomeParticles> getAmbientParticle() {
        return this.specialEffects.getAmbientParticleSettings();
    }

    public Optional<SoundEffect> getAmbientLoop() {
        return this.specialEffects.getAmbientLoopSoundEvent();
    }

    public Optional<CaveSoundSettings> getAmbientMood() {
        return this.specialEffects.getAmbientMoodSettings();
    }

    public Optional<CaveSound> getAmbientAdditions() {
        return this.specialEffects.getAmbientAdditionsSettings();
    }

    public Optional<Music> getBackgroundMusic() {
        return this.specialEffects.getBackgroundMusic();
    }

    public final BiomeBase.Geography getBiomeCategory() {
        return this.biomeCategory;
    }

    @Override
    public String toString() {
        MinecraftKey resourceLocation = RegistryGeneration.BIOME.getKey(this);
        return resourceLocation == null ? super.toString() : resourceLocation.toString();
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(BiomeBase.ClimateSettings.CODEC.forGetter((biome) -> {
                return biome.climateSettings;
            }), BiomeBase.Geography.CODEC.fieldOf("category").forGetter((biome) -> {
                return biome.biomeCategory;
            }), Codec.FLOAT.fieldOf("depth").forGetter((biome) -> {
                return biome.depth;
            }), Codec.FLOAT.fieldOf("scale").forGetter((biome) -> {
                return biome.scale;
            }), BiomeFog.CODEC.fieldOf("effects").forGetter((biome) -> {
                return biome.specialEffects;
            }), BiomeSettingsGeneration.CODEC.forGetter((biome) -> {
                return biome.generationSettings;
            }), BiomeSettingsMobs.CODEC.forGetter((biome) -> {
                return biome.mobSettings;
            })).apply(instance, BiomeBase::new);
        });
        NETWORK_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(BiomeBase.ClimateSettings.CODEC.forGetter((biome) -> {
                return biome.climateSettings;
            }), BiomeBase.Geography.CODEC.fieldOf("category").forGetter((biome) -> {
                return biome.biomeCategory;
            }), Codec.FLOAT.fieldOf("depth").forGetter((biome) -> {
                return biome.depth;
            }), Codec.FLOAT.fieldOf("scale").forGetter((biome) -> {
                return biome.scale;
            }), BiomeFog.CODEC.fieldOf("effects").forGetter((biome) -> {
                return biome.specialEffects;
            })).apply(instance, (climateSettings, biomeCategory, float_, float2, biomeSpecialEffects) -> {
                return new BiomeBase(climateSettings, biomeCategory, float_, float2, biomeSpecialEffects, BiomeSettingsGeneration.EMPTY, BiomeSettingsMobs.EMPTY);
            });
        });
    }

    public static class BiomeBuilder {
        @Nullable
        private BiomeBase.Precipitation precipitation;
        @Nullable
        private BiomeBase.Geography biomeCategory;
        @Nullable
        private Float depth;
        @Nullable
        private Float scale;
        @Nullable
        private Float temperature;
        private BiomeBase.TemperatureModifier temperatureModifier = BiomeBase.TemperatureModifier.NONE;
        @Nullable
        private Float downfall;
        @Nullable
        private BiomeFog specialEffects;
        @Nullable
        private BiomeSettingsMobs mobSpawnSettings;
        @Nullable
        private BiomeSettingsGeneration generationSettings;

        public BiomeBase.BiomeBuilder precipitation(BiomeBase.Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }

        public BiomeBase.BiomeBuilder biomeCategory(BiomeBase.Geography category) {
            this.biomeCategory = category;
            return this;
        }

        public BiomeBase.BiomeBuilder depth(float depth) {
            this.depth = depth;
            return this;
        }

        public BiomeBase.BiomeBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public BiomeBase.BiomeBuilder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public BiomeBase.BiomeBuilder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public BiomeBase.BiomeBuilder specialEffects(BiomeFog effects) {
            this.specialEffects = effects;
            return this;
        }

        public BiomeBase.BiomeBuilder mobSpawnSettings(BiomeSettingsMobs spawnSettings) {
            this.mobSpawnSettings = spawnSettings;
            return this;
        }

        public BiomeBase.BiomeBuilder generationSettings(BiomeSettingsGeneration generationSettings) {
            this.generationSettings = generationSettings;
            return this;
        }

        public BiomeBase.BiomeBuilder temperatureAdjustment(BiomeBase.TemperatureModifier temperatureModifier) {
            this.temperatureModifier = temperatureModifier;
            return this;
        }

        public BiomeBase build() {
            if (this.precipitation != null && this.biomeCategory != null && this.depth != null && this.scale != null && this.temperature != null && this.downfall != null && this.specialEffects != null && this.mobSpawnSettings != null && this.generationSettings != null) {
                return new BiomeBase(new BiomeBase.ClimateSettings(this.precipitation, this.temperature, this.temperatureModifier, this.downfall), this.biomeCategory, this.depth, this.scale, this.specialEffects, this.generationSettings, this.mobSpawnSettings);
            } else {
                throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
            }
        }

        @Override
        public String toString() {
            return "BiomeBuilder{\nprecipitation=" + this.precipitation + ",\nbiomeCategory=" + this.biomeCategory + ",\ndepth=" + this.depth + ",\nscale=" + this.scale + ",\ntemperature=" + this.temperature + ",\ntemperatureModifier=" + this.temperatureModifier + ",\ndownfall=" + this.downfall + ",\nspecialEffects=" + this.specialEffects + ",\nmobSpawnSettings=" + this.mobSpawnSettings + ",\ngenerationSettings=" + this.generationSettings + ",\n}";
        }
    }

    public static class ClimateParameters {
        public static final Codec<BiomeBase.ClimateParameters> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.floatRange(-2.0F, 2.0F).fieldOf("temperature").forGetter((climateParameters) -> {
                return climateParameters.temperature;
            }), Codec.floatRange(-2.0F, 2.0F).fieldOf("humidity").forGetter((climateParameters) -> {
                return climateParameters.humidity;
            }), Codec.floatRange(-2.0F, 2.0F).fieldOf("altitude").forGetter((climateParameters) -> {
                return climateParameters.altitude;
            }), Codec.floatRange(-2.0F, 2.0F).fieldOf("weirdness").forGetter((climateParameters) -> {
                return climateParameters.weirdness;
            }), Codec.floatRange(0.0F, 1.0F).fieldOf("offset").forGetter((climateParameters) -> {
                return climateParameters.offset;
            })).apply(instance, BiomeBase.ClimateParameters::new);
        });
        private final float temperature;
        private final float humidity;
        private final float altitude;
        private final float weirdness;
        private final float offset;

        public ClimateParameters(float temperature, float humidity, float altitude, float weirdness, float weight) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.altitude = altitude;
            this.weirdness = weirdness;
            this.offset = weight;
        }

        @Override
        public String toString() {
            return "temp: " + this.temperature + ", hum: " + this.humidity + ", alt: " + this.altitude + ", weird: " + this.weirdness + ", offset: " + this.offset;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                BiomeBase.ClimateParameters climateParameters = (BiomeBase.ClimateParameters)object;
                if (Float.compare(climateParameters.temperature, this.temperature) != 0) {
                    return false;
                } else if (Float.compare(climateParameters.humidity, this.humidity) != 0) {
                    return false;
                } else if (Float.compare(climateParameters.altitude, this.altitude) != 0) {
                    return false;
                } else {
                    return Float.compare(climateParameters.weirdness, this.weirdness) == 0;
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.temperature != 0.0F ? Float.floatToIntBits(this.temperature) : 0;
            i = 31 * i + (this.humidity != 0.0F ? Float.floatToIntBits(this.humidity) : 0);
            i = 31 * i + (this.altitude != 0.0F ? Float.floatToIntBits(this.altitude) : 0);
            return 31 * i + (this.weirdness != 0.0F ? Float.floatToIntBits(this.weirdness) : 0);
        }

        public float fitness(BiomeBase.ClimateParameters other) {
            return (this.temperature - other.temperature) * (this.temperature - other.temperature) + (this.humidity - other.humidity) * (this.humidity - other.humidity) + (this.altitude - other.altitude) * (this.altitude - other.altitude) + (this.weirdness - other.weirdness) * (this.weirdness - other.weirdness) + (this.offset - other.offset) * (this.offset - other.offset);
        }
    }

    static class ClimateSettings {
        public static final MapCodec<BiomeBase.ClimateSettings> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(BiomeBase.Precipitation.CODEC.fieldOf("precipitation").forGetter((climateSettings) -> {
                return climateSettings.precipitation;
            }), Codec.FLOAT.fieldOf("temperature").forGetter((climateSettings) -> {
                return climateSettings.temperature;
            }), BiomeBase.TemperatureModifier.CODEC.optionalFieldOf("temperature_modifier", BiomeBase.TemperatureModifier.NONE).forGetter((climateSettings) -> {
                return climateSettings.temperatureModifier;
            }), Codec.FLOAT.fieldOf("downfall").forGetter((climateSettings) -> {
                return climateSettings.downfall;
            })).apply(instance, BiomeBase.ClimateSettings::new);
        });
        final BiomeBase.Precipitation precipitation;
        final float temperature;
        final BiomeBase.TemperatureModifier temperatureModifier;
        final float downfall;

        ClimateSettings(BiomeBase.Precipitation precipitation, float f, BiomeBase.TemperatureModifier temperatureModifier, float g) {
            this.precipitation = precipitation;
            this.temperature = f;
            this.temperatureModifier = temperatureModifier;
            this.downfall = g;
        }
    }

    public static enum Geography implements INamable {
        NONE("none"),
        TAIGA("taiga"),
        EXTREME_HILLS("extreme_hills"),
        JUNGLE("jungle"),
        MESA("mesa"),
        PLAINS("plains"),
        SAVANNA("savanna"),
        ICY("icy"),
        THEEND("the_end"),
        BEACH("beach"),
        FOREST("forest"),
        OCEAN("ocean"),
        DESERT("desert"),
        RIVER("river"),
        SWAMP("swamp"),
        MUSHROOM("mushroom"),
        NETHER("nether"),
        UNDERGROUND("underground");

        public static final Codec<BiomeBase.Geography> CODEC = INamable.fromEnum(BiomeBase.Geography::values, BiomeBase.Geography::byName);
        private static final Map<String, BiomeBase.Geography> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(BiomeBase.Geography::getName, (category) -> {
            return category;
        }));
        private final String name;

        private Geography(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static BiomeBase.Geography byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum Precipitation implements INamable {
        NONE("none"),
        RAIN("rain"),
        SNOW("snow");

        public static final Codec<BiomeBase.Precipitation> CODEC = INamable.fromEnum(BiomeBase.Precipitation::values, BiomeBase.Precipitation::byName);
        private static final Map<String, BiomeBase.Precipitation> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(BiomeBase.Precipitation::getName, (precipitation) -> {
            return precipitation;
        }));
        private final String name;

        private Precipitation(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static BiomeBase.Precipitation byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum TemperatureModifier implements INamable {
        NONE("none") {
            @Override
            public float modifyTemperature(BlockPosition pos, float temperature) {
                return temperature;
            }
        },
        FROZEN("frozen") {
            @Override
            public float modifyTemperature(BlockPosition pos, float temperature) {
                double d = BiomeBase.FROZEN_TEMPERATURE_NOISE.getValue((double)pos.getX() * 0.05D, (double)pos.getZ() * 0.05D, false) * 7.0D;
                double e = BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.2D, (double)pos.getZ() * 0.2D, false);
                double f = d + e;
                if (f < 0.3D) {
                    double g = BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.09D, (double)pos.getZ() * 0.09D, false);
                    if (g < 0.8D) {
                        return 0.2F;
                    }
                }

                return temperature;
            }
        };

        private final String name;
        public static final Codec<BiomeBase.TemperatureModifier> CODEC = INamable.fromEnum(BiomeBase.TemperatureModifier::values, BiomeBase.TemperatureModifier::byName);
        private static final Map<String, BiomeBase.TemperatureModifier> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(BiomeBase.TemperatureModifier::getName, (temperatureModifier) -> {
            return temperatureModifier;
        }));

        public abstract float modifyTemperature(BlockPosition pos, float temperature);

        TemperatureModifier(String string2) {
            this.name = string2;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static BiomeBase.TemperatureModifier byName(String name) {
            return BY_NAME.get(name);
        }
    }
}
