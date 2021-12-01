package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundTrack;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
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
    private static final NoiseGenerator3 TEMPERATURE_NOISE = new NoiseGenerator3(new SeededRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
    static final NoiseGenerator3 FROZEN_TEMPERATURE_NOISE = new NoiseGenerator3(new SeededRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0));
    /** @deprecated */
    @Deprecated(
        forRemoval = true
    )
    public static final NoiseGenerator3 BIOME_INFO_NOISE = new NoiseGenerator3(new SeededRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));
    private static final int TEMPERATURE_CACHE_SIZE = 1024;
    private final BiomeBase.ClimateSettings climateSettings;
    private final BiomeSettingsGeneration generationSettings;
    private final BiomeSettingsMobs mobSettings;
    private final BiomeBase.Geography biomeCategory;
    private final BiomeFog specialEffects;
    private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
        return SystemUtils.make(() -> {
            Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
                protected void rehash(int i) {
                }
            };
            long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
            return long2FloatLinkedOpenHashMap;
        });
    });

    BiomeBase(BiomeBase.ClimateSettings weather, BiomeBase.Geography category, BiomeFog effects, BiomeSettingsGeneration generationSettings, BiomeSettingsMobs spawnSettings) {
        this.climateSettings = weather;
        this.generationSettings = generationSettings;
        this.mobSettings = spawnSettings;
        this.biomeCategory = category;
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
        if (pos.getY() > 80) {
            float g = (float)(TEMPERATURE_NOISE.getValue((double)((float)pos.getX() / 8.0F), (double)((float)pos.getZ() / 8.0F), false) * 8.0D);
            return f - (g + (float)pos.getY() - 80.0F) * 0.05F / 40.0F;
        } else {
            return f;
        }
    }

    /** @deprecated */
    @Deprecated
    public float getAdjustedTemperature(BlockPosition blockPos) {
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
        if (this.warmEnoughToRain(pos)) {
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

    public boolean coldEnoughToSnow(BlockPosition pos) {
        return !this.warmEnoughToRain(pos);
    }

    public boolean warmEnoughToRain(BlockPosition pos) {
        return this.getAdjustedTemperature(pos) >= 0.15F;
    }

    public boolean shouldMeltFrozenOceanIcebergSlightly(BlockPosition pos) {
        return this.getAdjustedTemperature(pos) > 0.1F;
    }

    public boolean shouldSnowGolemBurn(BlockPosition pos) {
        return this.getAdjustedTemperature(pos) > 1.0F;
    }

    public boolean shouldSnow(IWorldReader world, BlockPosition pos) {
        if (this.warmEnoughToRain(pos)) {
            return false;
        } else {
            if (pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && world.getBrightness(EnumSkyBlock.BLOCK, pos) < 10) {
                IBlockData blockState = world.getType(pos);
                if (blockState.isAir() && Blocks.SNOW.getBlockData().canPlace(world, pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public BiomeSettingsGeneration getGenerationSettings() {
        return this.generationSettings;
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

    public final float getHumidity() {
        return this.climateSettings.downfall;
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

    public Optional<SoundTrack> getBackgroundMusic() {
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
            }), BiomeFog.CODEC.fieldOf("effects").forGetter((biome) -> {
                return biome.specialEffects;
            })).apply(instance, (climateSettings, biomeCategory, biomeSpecialEffects) -> {
                return new BiomeBase(climateSettings, biomeCategory, biomeSpecialEffects, BiomeSettingsGeneration.EMPTY, BiomeSettingsMobs.EMPTY);
            });
        });
    }

    public static class BiomeBuilder {
        @Nullable
        private BiomeBase.Precipitation precipitation;
        @Nullable
        private BiomeBase.Geography biomeCategory;
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
            if (this.precipitation != null && this.biomeCategory != null && this.temperature != null && this.downfall != null && this.specialEffects != null && this.mobSpawnSettings != null && this.generationSettings != null) {
                return new BiomeBase(new BiomeBase.ClimateSettings(this.precipitation, this.temperature, this.temperatureModifier, this.downfall), this.biomeCategory, this.specialEffects, this.generationSettings, this.mobSpawnSettings);
            } else {
                throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
            }
        }

        @Override
        public String toString() {
            return "BiomeBuilder{\nprecipitation=" + this.precipitation + ",\nbiomeCategory=" + this.biomeCategory + ",\ntemperature=" + this.temperature + ",\ntemperatureModifier=" + this.temperatureModifier + ",\ndownfall=" + this.downfall + ",\nspecialEffects=" + this.specialEffects + ",\nmobSpawnSettings=" + this.mobSpawnSettings + ",\ngenerationSettings=" + this.generationSettings + ",\n}";
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

        ClimateSettings(BiomeBase.Precipitation precipitation, float temperature, BiomeBase.TemperatureModifier temperatureModifier, float downfall) {
            this.precipitation = precipitation;
            this.temperature = temperature;
            this.temperatureModifier = temperatureModifier;
            this.downfall = downfall;
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
        UNDERGROUND("underground"),
        MOUNTAIN("mountain");

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

        TemperatureModifier(String name) {
            this.name = name;
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
