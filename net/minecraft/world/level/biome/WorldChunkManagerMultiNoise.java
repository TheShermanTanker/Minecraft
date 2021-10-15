package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;

public class WorldChunkManagerMultiNoise extends WorldChunkManager {
    private static final WorldChunkManagerMultiNoise.NoiseParameters DEFAULT_NOISE_PARAMETERS = new WorldChunkManagerMultiNoise.NoiseParameters(-7, ImmutableList.of(1.0D, 1.0D));
    public static final MapCodec<WorldChunkManagerMultiNoise> DIRECT_CODEC;
    public static final Codec<WorldChunkManagerMultiNoise> CODEC = Codec.mapEither(WorldChunkManagerMultiNoise.PresetInstance.CODEC, DIRECT_CODEC).xmap((either) -> {
        return either.map(WorldChunkManagerMultiNoise.PresetInstance::biomeSource, Function.identity());
    }, (multiNoiseBiomeSource) -> {
        return multiNoiseBiomeSource.preset().map(Either::left).orElseGet(() -> {
            return Either.right(multiNoiseBiomeSource);
        });
    }).codec();
    private final WorldChunkManagerMultiNoise.NoiseParameters temperatureParams;
    private final WorldChunkManagerMultiNoise.NoiseParameters humidityParams;
    private final WorldChunkManagerMultiNoise.NoiseParameters altitudeParams;
    private final WorldChunkManagerMultiNoise.NoiseParameters weirdnessParams;
    private final NoiseGeneratorNormal temperatureNoise;
    private final NoiseGeneratorNormal humidityNoise;
    private final NoiseGeneratorNormal altitudeNoise;
    private final NoiseGeneratorNormal weirdnessNoise;
    private final List<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> parameters;
    private final boolean useY;
    private final long seed;
    private final Optional<Pair<IRegistry<BiomeBase>, WorldChunkManagerMultiNoise.Preset>> preset;

    public WorldChunkManagerMultiNoise(long seed, List<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> biomePoints) {
        this(seed, biomePoints, Optional.empty());
    }

    WorldChunkManagerMultiNoise(long seed, List<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> biomePoints, Optional<Pair<IRegistry<BiomeBase>, WorldChunkManagerMultiNoise.Preset>> instance) {
        this(seed, biomePoints, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, DEFAULT_NOISE_PARAMETERS, instance);
    }

    private WorldChunkManagerMultiNoise(long seed, List<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> biomePoints, WorldChunkManagerMultiNoise.NoiseParameters temperatureNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters humidityNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters altitudeNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters weirdnessNoiseParameters) {
        this(seed, biomePoints, temperatureNoiseParameters, humidityNoiseParameters, altitudeNoiseParameters, weirdnessNoiseParameters, Optional.empty());
    }

    private WorldChunkManagerMultiNoise(long seed, List<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> biomePoints, WorldChunkManagerMultiNoise.NoiseParameters temperatureNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters humidityNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters altitudeNoiseParameters, WorldChunkManagerMultiNoise.NoiseParameters weirdnessNoiseParameters, Optional<Pair<IRegistry<BiomeBase>, WorldChunkManagerMultiNoise.Preset>> instance) {
        super(biomePoints.stream().map(Pair::getSecond));
        this.seed = seed;
        this.preset = instance;
        this.temperatureParams = temperatureNoiseParameters;
        this.humidityParams = humidityNoiseParameters;
        this.altitudeParams = altitudeNoiseParameters;
        this.weirdnessParams = weirdnessNoiseParameters;
        this.temperatureNoise = NoiseGeneratorNormal.create(new SeededRandom(seed), temperatureNoiseParameters.firstOctave(), temperatureNoiseParameters.amplitudes());
        this.humidityNoise = NoiseGeneratorNormal.create(new SeededRandom(seed + 1L), humidityNoiseParameters.firstOctave(), humidityNoiseParameters.amplitudes());
        this.altitudeNoise = NoiseGeneratorNormal.create(new SeededRandom(seed + 2L), altitudeNoiseParameters.firstOctave(), altitudeNoiseParameters.amplitudes());
        this.weirdnessNoise = NoiseGeneratorNormal.create(new SeededRandom(seed + 3L), weirdnessNoiseParameters.firstOctave(), weirdnessNoiseParameters.amplitudes());
        this.parameters = biomePoints;
        this.useY = false;
    }

    public static WorldChunkManagerMultiNoise overworld(IRegistry<BiomeBase> registry, long l) {
        ImmutableList<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> immutableList = parameters(registry);
        WorldChunkManagerMultiNoise.NoiseParameters noiseParameters = new WorldChunkManagerMultiNoise.NoiseParameters(-9, 1.0D, 0.0D, 3.0D, 3.0D, 3.0D, 3.0D);
        WorldChunkManagerMultiNoise.NoiseParameters noiseParameters2 = new WorldChunkManagerMultiNoise.NoiseParameters(-7, 1.0D, 2.0D, 4.0D, 4.0D);
        WorldChunkManagerMultiNoise.NoiseParameters noiseParameters3 = new WorldChunkManagerMultiNoise.NoiseParameters(-9, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0D);
        WorldChunkManagerMultiNoise.NoiseParameters noiseParameters4 = new WorldChunkManagerMultiNoise.NoiseParameters(-8, 1.2D, 0.6D, 0.0D, 0.0D, 1.0D, 0.0D);
        return new WorldChunkManagerMultiNoise(l, immutableList, noiseParameters, noiseParameters2, noiseParameters3, noiseParameters4, Optional.empty());
    }

    @Override
    protected Codec<? extends WorldChunkManager> codec() {
        return CODEC;
    }

    @Override
    public WorldChunkManager withSeed(long seed) {
        return new WorldChunkManagerMultiNoise(seed, this.parameters, this.temperatureParams, this.humidityParams, this.altitudeParams, this.weirdnessParams, this.preset);
    }

    private Optional<WorldChunkManagerMultiNoise.PresetInstance> preset() {
        return this.preset.map((pair) -> {
            return new WorldChunkManagerMultiNoise.PresetInstance(pair.getSecond(), pair.getFirst(), this.seed);
        });
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        int i = this.useY ? biomeY : 0;
        BiomeBase.ClimateParameters climateParameters = new BiomeBase.ClimateParameters((float)this.temperatureNoise.getValue((double)biomeX, (double)i, (double)biomeZ), (float)this.humidityNoise.getValue((double)biomeX, (double)i, (double)biomeZ), (float)this.altitudeNoise.getValue((double)biomeX, (double)i, (double)biomeZ), (float)this.weirdnessNoise.getValue((double)biomeX, (double)i, (double)biomeZ), 0.0F);
        return this.parameters.stream().min(Comparator.comparing((pair) -> {
            return pair.getFirst().fitness(climateParameters);
        })).map(Pair::getSecond).map(Supplier::get).orElse(BiomeRegistry.THE_VOID);
    }

    public static ImmutableList<Pair<BiomeBase.ClimateParameters, Supplier<BiomeBase>>> parameters(IRegistry<BiomeBase> registry) {
        return ImmutableList.of(Pair.of(new BiomeBase.ClimateParameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
            return registry.getOrThrow(Biomes.PLAINS);
        }));
    }

    public boolean stable(long seed) {
        return this.seed == seed && this.preset.isPresent() && Objects.equals(this.preset.get().getSecond(), WorldChunkManagerMultiNoise.Preset.NETHER);
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(Codec.LONG.fieldOf("seed").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.seed;
            }), RecordCodecBuilder.create((instancex) -> {
                return instancex.group(BiomeBase.ClimateParameters.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), BiomeBase.CODEC.fieldOf("biome").forGetter(Pair::getSecond)).apply(instancex, Pair::of);
            }).listOf().fieldOf("biomes").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.parameters;
            }), WorldChunkManagerMultiNoise.NoiseParameters.CODEC.fieldOf("temperature_noise").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.temperatureParams;
            }), WorldChunkManagerMultiNoise.NoiseParameters.CODEC.fieldOf("humidity_noise").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.humidityParams;
            }), WorldChunkManagerMultiNoise.NoiseParameters.CODEC.fieldOf("altitude_noise").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.altitudeParams;
            }), WorldChunkManagerMultiNoise.NoiseParameters.CODEC.fieldOf("weirdness_noise").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.weirdnessParams;
            })).apply(instance, WorldChunkManagerMultiNoise::new);
        });
    }

    static class NoiseParameters {
        private final int firstOctave;
        private final DoubleList amplitudes;
        public static final Codec<WorldChunkManagerMultiNoise.NoiseParameters> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("firstOctave").forGetter(WorldChunkManagerMultiNoise.NoiseParameters::firstOctave), Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(WorldChunkManagerMultiNoise.NoiseParameters::amplitudes)).apply(instance, WorldChunkManagerMultiNoise.NoiseParameters::new);
        });

        public NoiseParameters(int firstOctave, List<Double> amplitudes) {
            this.firstOctave = firstOctave;
            this.amplitudes = new DoubleArrayList(amplitudes);
        }

        public NoiseParameters(int firstOctave, double... amplitudes) {
            this.firstOctave = firstOctave;
            this.amplitudes = new DoubleArrayList(amplitudes);
        }

        public int firstOctave() {
            return this.firstOctave;
        }

        public DoubleList amplitudes() {
            return this.amplitudes;
        }
    }

    public static class Preset {
        static final Map<MinecraftKey, WorldChunkManagerMultiNoise.Preset> BY_NAME = Maps.newHashMap();
        public static final WorldChunkManagerMultiNoise.Preset NETHER = new WorldChunkManagerMultiNoise.Preset(new MinecraftKey("nether"), (preset, biomeRegistry, seed) -> {
            return new WorldChunkManagerMultiNoise(seed, ImmutableList.of(Pair.of(new BiomeBase.ClimateParameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
                return biomeRegistry.getOrThrow(Biomes.NETHER_WASTES);
            }), Pair.of(new BiomeBase.ClimateParameters(0.0F, -0.5F, 0.0F, 0.0F, 0.0F), () -> {
                return biomeRegistry.getOrThrow(Biomes.SOUL_SAND_VALLEY);
            }), Pair.of(new BiomeBase.ClimateParameters(0.4F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
                return biomeRegistry.getOrThrow(Biomes.CRIMSON_FOREST);
            }), Pair.of(new BiomeBase.ClimateParameters(0.0F, 0.5F, 0.0F, 0.0F, 0.375F), () -> {
                return biomeRegistry.getOrThrow(Biomes.WARPED_FOREST);
            }), Pair.of(new BiomeBase.ClimateParameters(-0.5F, 0.0F, 0.0F, 0.0F, 0.175F), () -> {
                return biomeRegistry.getOrThrow(Biomes.BASALT_DELTAS);
            })), Optional.of(Pair.of(biomeRegistry, preset)));
        });
        final MinecraftKey name;
        private final Function3<WorldChunkManagerMultiNoise.Preset, IRegistry<BiomeBase>, Long, WorldChunkManagerMultiNoise> biomeSource;

        public Preset(MinecraftKey id, Function3<WorldChunkManagerMultiNoise.Preset, IRegistry<BiomeBase>, Long, WorldChunkManagerMultiNoise> biomeSourceFunction) {
            this.name = id;
            this.biomeSource = biomeSourceFunction;
            BY_NAME.put(id, this);
        }

        public WorldChunkManagerMultiNoise biomeSource(IRegistry<BiomeBase> biomeRegistry, long seed) {
            return this.biomeSource.apply(this, biomeRegistry, seed);
        }
    }

    static final class PresetInstance {
        public static final MapCodec<WorldChunkManagerMultiNoise.PresetInstance> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(MinecraftKey.CODEC.flatXmap((id) -> {
                return Optional.ofNullable(WorldChunkManagerMultiNoise.Preset.BY_NAME.get(id)).map(DataResult::success).orElseGet(() -> {
                    return DataResult.error("Unknown preset: " + id);
                });
            }, (preset) -> {
                return DataResult.success(preset.name);
            }).fieldOf("preset").stable().forGetter(WorldChunkManagerMultiNoise.PresetInstance::preset), RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).forGetter(WorldChunkManagerMultiNoise.PresetInstance::biomes), Codec.LONG.fieldOf("seed").stable().forGetter(WorldChunkManagerMultiNoise.PresetInstance::seed)).apply(instance, instance.stable(WorldChunkManagerMultiNoise.PresetInstance::new));
        });
        private final WorldChunkManagerMultiNoise.Preset preset;
        private final IRegistry<BiomeBase> biomes;
        private final long seed;

        PresetInstance(WorldChunkManagerMultiNoise.Preset preset, IRegistry<BiomeBase> biomeRegistry, long seed) {
            this.preset = preset;
            this.biomes = biomeRegistry;
            this.seed = seed;
        }

        public WorldChunkManagerMultiNoise.Preset preset() {
            return this.preset;
        }

        public IRegistry<BiomeBase> biomes() {
            return this.biomes;
        }

        public long seed() {
            return this.seed;
        }

        public WorldChunkManagerMultiNoise biomeSource() {
            return this.preset.biomeSource(this.biomes, this.seed);
        }
    }
}
