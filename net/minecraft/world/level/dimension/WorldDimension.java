package net.minecraft.world.level.dimension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.biome.WorldChunkManagerMultiNoise;
import net.minecraft.world.level.biome.WorldChunkManagerTheEnd;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;

public final class WorldDimension {
    public static final Codec<WorldDimension> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(DimensionManager.CODEC.fieldOf("type").flatXmap(ExtraCodecs.nonNullSupplierCheck(), ExtraCodecs.nonNullSupplierCheck()).forGetter(WorldDimension::typeSupplier), ChunkGenerator.CODEC.fieldOf("generator").forGetter(WorldDimension::generator)).apply(instance, instance.stable(WorldDimension::new));
    });
    public static final ResourceKey<WorldDimension> OVERWORLD = ResourceKey.create(IRegistry.LEVEL_STEM_REGISTRY, new MinecraftKey("overworld"));
    public static final ResourceKey<WorldDimension> NETHER = ResourceKey.create(IRegistry.LEVEL_STEM_REGISTRY, new MinecraftKey("the_nether"));
    public static final ResourceKey<WorldDimension> END = ResourceKey.create(IRegistry.LEVEL_STEM_REGISTRY, new MinecraftKey("the_end"));
    private static final Set<ResourceKey<WorldDimension>> BUILTIN_ORDER = Sets.newLinkedHashSet(ImmutableList.of(OVERWORLD, NETHER, END));
    private final Supplier<DimensionManager> type;
    private final ChunkGenerator generator;

    public WorldDimension(Supplier<DimensionManager> typeSupplier, ChunkGenerator chunkGenerator) {
        this.type = typeSupplier;
        this.generator = chunkGenerator;
    }

    public Supplier<DimensionManager> typeSupplier() {
        return this.type;
    }

    public DimensionManager type() {
        return this.type.get();
    }

    public ChunkGenerator generator() {
        return this.generator;
    }

    public static RegistryMaterials<WorldDimension> sortMap(RegistryMaterials<WorldDimension> mappedRegistry) {
        RegistryMaterials<WorldDimension> mappedRegistry2 = new RegistryMaterials<>(IRegistry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());

        for(ResourceKey<WorldDimension> resourceKey : BUILTIN_ORDER) {
            WorldDimension levelStem = mappedRegistry.get(resourceKey);
            if (levelStem != null) {
                mappedRegistry2.register(resourceKey, levelStem, mappedRegistry.lifecycle(levelStem));
            }
        }

        for(Entry<ResourceKey<WorldDimension>, WorldDimension> entry : mappedRegistry.entrySet()) {
            ResourceKey<WorldDimension> resourceKey2 = entry.getKey();
            if (!BUILTIN_ORDER.contains(resourceKey2)) {
                mappedRegistry2.register(resourceKey2, entry.getValue(), mappedRegistry.lifecycle(entry.getValue()));
            }
        }

        return mappedRegistry2;
    }

    public static boolean stable(long seed, RegistryMaterials<WorldDimension> options) {
        List<Entry<ResourceKey<WorldDimension>, WorldDimension>> list = Lists.newArrayList(options.entrySet());
        if (list.size() != BUILTIN_ORDER.size()) {
            return false;
        } else {
            Entry<ResourceKey<WorldDimension>, WorldDimension> entry = list.get(0);
            Entry<ResourceKey<WorldDimension>, WorldDimension> entry2 = list.get(1);
            Entry<ResourceKey<WorldDimension>, WorldDimension> entry3 = list.get(2);
            if (entry.getKey() == OVERWORLD && entry2.getKey() == NETHER && entry3.getKey() == END) {
                if (!entry.getValue().type().equalTo(DimensionManager.DEFAULT_OVERWORLD) && entry.getValue().type() != DimensionManager.DEFAULT_OVERWORLD_CAVES) {
                    return false;
                } else if (!entry2.getValue().type().equalTo(DimensionManager.DEFAULT_NETHER)) {
                    return false;
                } else if (!entry3.getValue().type().equalTo(DimensionManager.DEFAULT_END)) {
                    return false;
                } else if (entry2.getValue().generator() instanceof ChunkGeneratorAbstract && entry3.getValue().generator() instanceof ChunkGeneratorAbstract) {
                    ChunkGeneratorAbstract noiseBasedChunkGenerator = (ChunkGeneratorAbstract)entry2.getValue().generator();
                    ChunkGeneratorAbstract noiseBasedChunkGenerator2 = (ChunkGeneratorAbstract)entry3.getValue().generator();
                    if (!noiseBasedChunkGenerator.stable(seed, GeneratorSettingBase.NETHER)) {
                        return false;
                    } else if (!noiseBasedChunkGenerator2.stable(seed, GeneratorSettingBase.END)) {
                        return false;
                    } else if (!(noiseBasedChunkGenerator.getWorldChunkManager() instanceof WorldChunkManagerMultiNoise)) {
                        return false;
                    } else {
                        WorldChunkManagerMultiNoise multiNoiseBiomeSource = (WorldChunkManagerMultiNoise)noiseBasedChunkGenerator.getWorldChunkManager();
                        if (!multiNoiseBiomeSource.stable(WorldChunkManagerMultiNoise.Preset.NETHER)) {
                            return false;
                        } else {
                            WorldChunkManager biomeSource = entry.getValue().generator().getWorldChunkManager();
                            if (biomeSource instanceof WorldChunkManagerMultiNoise && !((WorldChunkManagerMultiNoise)biomeSource).stable(WorldChunkManagerMultiNoise.Preset.OVERWORLD)) {
                                return false;
                            } else if (!(noiseBasedChunkGenerator2.getWorldChunkManager() instanceof WorldChunkManagerTheEnd)) {
                                return false;
                            } else {
                                WorldChunkManagerTheEnd theEndBiomeSource = (WorldChunkManagerTheEnd)noiseBasedChunkGenerator2.getWorldChunkManager();
                                return theEndBiomeSource.stable(seed);
                            }
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
