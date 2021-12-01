package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.TerrainInfo;
import net.minecraft.world.level.levelgen.blending.Blender;

public class WorldChunkManagerMultiNoise extends WorldChunkManager {
    public static final MapCodec<WorldChunkManagerMultiNoise> DIRECT_CODEC;
    public static final Codec<WorldChunkManagerMultiNoise> CODEC = Codec.mapEither(WorldChunkManagerMultiNoise.PresetInstance.CODEC, DIRECT_CODEC).xmap((either) -> {
        return either.map(WorldChunkManagerMultiNoise.PresetInstance::biomeSource, Function.identity());
    }, (multiNoiseBiomeSource) -> {
        return multiNoiseBiomeSource.preset().map(Either::left).orElseGet(() -> {
            return Either.right(multiNoiseBiomeSource);
        });
    }).codec();
    private final Climate.ParameterList<Supplier<BiomeBase>> parameters;
    private final Optional<WorldChunkManagerMultiNoise.PresetInstance> preset;

    private WorldChunkManagerMultiNoise(Climate.ParameterList<Supplier<BiomeBase>> entries) {
        this(entries, Optional.empty());
    }

    WorldChunkManagerMultiNoise(Climate.ParameterList<Supplier<BiomeBase>> biomeEntries, Optional<WorldChunkManagerMultiNoise.PresetInstance> instance) {
        super(biomeEntries.values().stream().map(Pair::getSecond));
        this.preset = instance;
        this.parameters = biomeEntries;
    }

    @Override
    protected Codec<? extends WorldChunkManager> codec() {
        return CODEC;
    }

    @Override
    public WorldChunkManager withSeed(long seed) {
        return this;
    }

    private Optional<WorldChunkManagerMultiNoise.PresetInstance> preset() {
        return this.preset;
    }

    public boolean stable(WorldChunkManagerMultiNoise.Preset instance) {
        return this.preset.isPresent() && Objects.equals(this.preset.get().preset(), instance);
    }

    @Override
    public BiomeBase getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return this.getNoiseBiome(noise.sample(x, y, z));
    }

    @VisibleForDebug
    public BiomeBase getNoiseBiome(Climate.TargetPoint point) {
        return this.parameters.findValue(point, () -> {
            return BiomeRegistry.THE_VOID;
        }).get();
    }

    @Override
    public void addMultinoiseDebugInfo(List<String> info, BlockPosition pos, Climate.Sampler noiseSampler) {
        int i = QuartPos.fromBlock(pos.getX());
        int j = QuartPos.fromBlock(pos.getY());
        int k = QuartPos.fromBlock(pos.getZ());
        Climate.TargetPoint targetPoint = noiseSampler.sample(i, j, k);
        float f = Climate.unquantizeCoord(targetPoint.continentalness());
        float g = Climate.unquantizeCoord(targetPoint.erosion());
        float h = Climate.unquantizeCoord(targetPoint.temperature());
        float l = Climate.unquantizeCoord(targetPoint.humidity());
        float m = Climate.unquantizeCoord(targetPoint.weirdness());
        double d = (double)TerrainShaper.peaksAndValleys(m);
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        info.add("Multinoise C: " + decimalFormat.format((double)f) + " E: " + decimalFormat.format((double)g) + " T: " + decimalFormat.format((double)h) + " H: " + decimalFormat.format((double)l) + " W: " + decimalFormat.format((double)m));
        OverworldBiomeBuilder overworldBiomeBuilder = new OverworldBiomeBuilder();
        info.add("Biome builder PV: " + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(d) + " C: " + overworldBiomeBuilder.getDebugStringForContinentalness((double)f) + " E: " + overworldBiomeBuilder.getDebugStringForErosion((double)g) + " T: " + overworldBiomeBuilder.getDebugStringForTemperature((double)h) + " H: " + overworldBiomeBuilder.getDebugStringForHumidity((double)l));
        if (noiseSampler instanceof NoiseSampler) {
            NoiseSampler noiseSampler2 = (NoiseSampler)noiseSampler;
            TerrainInfo terrainInfo = noiseSampler2.terrainInfo(pos.getX(), pos.getZ(), f, m, g, Blender.empty());
            info.add("Terrain PV: " + decimalFormat.format(d) + " O: " + decimalFormat.format(terrainInfo.offset()) + " F: " + decimalFormat.format(terrainInfo.factor()) + " JA: " + decimalFormat.format(terrainInfo.jaggedness()));
        }
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(ExtraCodecs.<Pair<Climate.ParameterPoint, T>>nonEmptyList(RecordCodecBuilder.create((instancex) -> {
                return instancex.group(Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), BiomeBase.CODEC.fieldOf("biome").forGetter(Pair::getSecond)).apply(instancex, Pair::of);
            }).listOf()).xmap(Climate.ParameterList::new, Climate.ParameterList::values).fieldOf("biomes").forGetter((multiNoiseBiomeSource) -> {
                return multiNoiseBiomeSource.parameters;
            })).apply(instance, WorldChunkManagerMultiNoise::new);
        });
    }

    public static class Preset {
        static final Map<MinecraftKey, WorldChunkManagerMultiNoise.Preset> BY_NAME = Maps.newHashMap();
        public static final WorldChunkManagerMultiNoise.Preset NETHER = new WorldChunkManagerMultiNoise.Preset(new MinecraftKey("nether"), (registry) -> {
            return new Climate.ParameterList<>(ImmutableList.of(Pair.of(Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
                return registry.getOrThrow(Biomes.NETHER_WASTES);
            }), Pair.of(Climate.parameters(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
                return registry.getOrThrow(Biomes.SOUL_SAND_VALLEY);
            }), Pair.of(Climate.parameters(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), () -> {
                return registry.getOrThrow(Biomes.CRIMSON_FOREST);
            }), Pair.of(Climate.parameters(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.375F), () -> {
                return registry.getOrThrow(Biomes.WARPED_FOREST);
            }), Pair.of(Climate.parameters(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.175F), () -> {
                return registry.getOrThrow(Biomes.BASALT_DELTAS);
            })));
        });
        public static final WorldChunkManagerMultiNoise.Preset OVERWORLD = new WorldChunkManagerMultiNoise.Preset(new MinecraftKey("overworld"), (registry) -> {
            Builder<Pair<Climate.ParameterPoint, Supplier<BiomeBase>>> builder = ImmutableList.builder();
            (new OverworldBiomeBuilder()).addBiomes((pair) -> {
                builder.add(pair.mapSecond((resourceKey) -> {
                    return () -> {
                        return registry.getOrThrow(resourceKey);
                    };
                }));
            });
            return new Climate.ParameterList<>(builder.build());
        });
        final MinecraftKey name;
        private final Function<IRegistry<BiomeBase>, Climate.ParameterList<Supplier<BiomeBase>>> parameterSource;

        public Preset(MinecraftKey id, Function<IRegistry<BiomeBase>, Climate.ParameterList<Supplier<BiomeBase>>> biomeSourceFunction) {
            this.name = id;
            this.parameterSource = biomeSourceFunction;
            BY_NAME.put(id, this);
        }

        WorldChunkManagerMultiNoise biomeSource(WorldChunkManagerMultiNoise.PresetInstance instance, boolean useInstance) {
            Climate.ParameterList<Supplier<BiomeBase>> parameterList = this.parameterSource.apply(instance.biomes());
            return new WorldChunkManagerMultiNoise(parameterList, useInstance ? Optional.of(instance) : Optional.empty());
        }

        public WorldChunkManagerMultiNoise biomeSource(IRegistry<BiomeBase> biomeRegistry, boolean useInstance) {
            return this.biomeSource(new WorldChunkManagerMultiNoise.PresetInstance(this, biomeRegistry), useInstance);
        }

        public WorldChunkManagerMultiNoise biomeSource(IRegistry<BiomeBase> biomeRegistry) {
            return this.biomeSource(biomeRegistry, true);
        }
    }

    static record PresetInstance(WorldChunkManagerMultiNoise.Preset preset, IRegistry<BiomeBase> biomes) {
        public static final MapCodec<WorldChunkManagerMultiNoise.PresetInstance> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(MinecraftKey.CODEC.flatXmap((id) -> {
                return Optional.ofNullable(WorldChunkManagerMultiNoise.Preset.BY_NAME.get(id)).map(DataResult::success).orElseGet(() -> {
                    return DataResult.error("Unknown preset: " + id);
                });
            }, (preset) -> {
                return DataResult.success(preset.name);
            }).fieldOf("preset").stable().forGetter(WorldChunkManagerMultiNoise.PresetInstance::preset), RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).forGetter(WorldChunkManagerMultiNoise.PresetInstance::biomes)).apply(instance, instance.stable(WorldChunkManagerMultiNoise.PresetInstance::new));
        });

        PresetInstance(WorldChunkManagerMultiNoise.Preset preset, IRegistry<BiomeBase> biomeRegistry) {
            this.preset = preset;
            this.biomes = biomeRegistry;
        }

        public WorldChunkManagerMultiNoise biomeSource() {
            return this.preset.biomeSource(this, true);
        }

        public WorldChunkManagerMultiNoise.Preset preset() {
            return this.preset;
        }

        public IRegistry<BiomeBase> biomes() {
            return this.biomes;
        }
    }
}
