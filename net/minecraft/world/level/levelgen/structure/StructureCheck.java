package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.IChunkLoader;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureCheck {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int NO_STRUCTURE = -1;
    private final ChunkScanAccess storageAccess;
    private final IRegistryCustom registryAccess;
    private final IRegistry<BiomeBase> biomes;
    private final DefinedStructureManager structureManager;
    private final ResourceKey<World> dimension;
    private final ChunkGenerator chunkGenerator;
    private final IWorldHeightAccess heightAccessor;
    private final WorldChunkManager biomeSource;
    private final long seed;
    private final DataFixer fixerUpper;
    private final Long2ObjectMap<Object2IntMap<StructureGenerator<?>>> loadedChunks = new Long2ObjectOpenHashMap<>();
    private final Map<StructureGenerator<?>, Long2BooleanMap> featureChecks = new HashMap<>();

    public StructureCheck(ChunkScanAccess chunkIoWorker, IRegistryCustom registryManager, DefinedStructureManager structureManager, ResourceKey<World> worldKey, ChunkGenerator chunkGenerator, IWorldHeightAccess world, WorldChunkManager biomeSource, long seed, DataFixer dataFixer) {
        this.storageAccess = chunkIoWorker;
        this.registryAccess = registryManager;
        this.structureManager = structureManager;
        this.dimension = worldKey;
        this.chunkGenerator = chunkGenerator;
        this.heightAccessor = world;
        this.biomeSource = biomeSource;
        this.seed = seed;
        this.fixerUpper = dataFixer;
        this.biomes = registryManager.ownedRegistryOrThrow(IRegistry.BIOME_REGISTRY);
    }

    public <F extends StructureGenerator<?>> StructureCheckResult checkStart(ChunkCoordIntPair pos, F feature, boolean skipExistingChunk) {
        long l = pos.pair();
        Object2IntMap<StructureGenerator<?>> object2IntMap = this.loadedChunks.get(l);
        if (object2IntMap != null) {
            return this.checkStructureInfo(object2IntMap, feature, skipExistingChunk);
        } else {
            StructureCheckResult structureCheckResult = this.tryLoadFromStorage(pos, feature, skipExistingChunk, l);
            if (structureCheckResult != null) {
                return structureCheckResult;
            } else {
                boolean bl = this.featureChecks.computeIfAbsent(feature, (featurex) -> {
                    return new Long2BooleanOpenHashMap();
                }).computeIfAbsent(l, (posx) -> {
                    Multimap<StructureFeature<?, ?>, ResourceKey<BiomeBase>> multimap = this.chunkGenerator.getSettings().structures(feature);

                    for(Entry<StructureFeature<?, ?>, Collection<ResourceKey<BiomeBase>>> entry : multimap.asMap().entrySet()) {
                        if (this.canCreateStructure(pos, entry.getKey(), entry.getValue())) {
                            return true;
                        }
                    }

                    return false;
                });
                return !bl ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
            }
        }
    }

    private <FC extends WorldGenFeatureConfiguration, F extends StructureGenerator<FC>> boolean canCreateStructure(ChunkCoordIntPair pos, StructureFeature<FC, F> feature, Collection<ResourceKey<BiomeBase>> allowedBiomes) {
        Predicate<BiomeBase> predicate = (biome) -> {
            return this.biomes.getResourceKey(biome).filter(allowedBiomes::contains).isPresent();
        };
        return feature.feature.canGenerate(this.registryAccess, this.chunkGenerator, this.biomeSource, this.structureManager, this.seed, pos, feature.config, this.heightAccessor, predicate);
    }

    @Nullable
    private StructureCheckResult tryLoadFromStorage(ChunkCoordIntPair pos, StructureGenerator<?> feature, boolean skipExistingChunk, long posLong) {
        CollectFields collectFields = new CollectFields(new CollectFields.WantedField(NBTTagInt.TYPE, "DataVersion"), new CollectFields.WantedField("Level", "Structures", NBTTagCompound.TYPE, "Starts"), new CollectFields.WantedField("structures", NBTTagCompound.TYPE, "starts"));

        try {
            this.storageAccess.scanChunk(pos, collectFields).join();
        } catch (Exception var13) {
            LOGGER.warn("Failed to read chunk {}", pos, var13);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
        }

        NBTBase tag = collectFields.getResult();
        if (!(tag instanceof NBTTagCompound)) {
            return null;
        } else {
            NBTTagCompound compoundTag = (NBTTagCompound)tag;
            int i = IChunkLoader.getVersion(compoundTag);
            if (i <= 1493) {
                return StructureCheckResult.CHUNK_LOAD_NEEDED;
            } else {
                IChunkLoader.injectDatafixingContext(compoundTag, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

                NBTTagCompound compoundTag2;
                try {
                    compoundTag2 = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, compoundTag, i);
                } catch (Exception var12) {
                    LOGGER.warn("Failed to partially datafix chunk {}", pos, var12);
                    return StructureCheckResult.CHUNK_LOAD_NEEDED;
                }

                Object2IntMap<StructureGenerator<?>> object2IntMap = this.loadStructures(compoundTag2);
                if (object2IntMap == null) {
                    return null;
                } else {
                    this.storeFullResults(posLong, object2IntMap);
                    return this.checkStructureInfo(object2IntMap, feature, skipExistingChunk);
                }
            }
        }
    }

    @Nullable
    private Object2IntMap<StructureGenerator<?>> loadStructures(NBTTagCompound nbt) {
        if (!nbt.hasKeyOfType("structures", 10)) {
            return null;
        } else {
            NBTTagCompound compoundTag = nbt.getCompound("structures");
            if (!compoundTag.hasKeyOfType("starts", 10)) {
                return null;
            } else {
                NBTTagCompound compoundTag2 = compoundTag.getCompound("starts");
                if (compoundTag2.isEmpty()) {
                    return Object2IntMaps.emptyMap();
                } else {
                    Object2IntMap<StructureGenerator<?>> object2IntMap = new Object2IntOpenHashMap<>();

                    for(String string : compoundTag2.getKeys()) {
                        String string2 = string.toLowerCase(Locale.ROOT);
                        StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string2);
                        if (structureFeature != null) {
                            NBTTagCompound compoundTag3 = compoundTag2.getCompound(string);
                            if (!compoundTag3.isEmpty()) {
                                String string3 = compoundTag3.getString("id");
                                if (!"INVALID".equals(string3)) {
                                    int i = compoundTag3.getInt("references");
                                    object2IntMap.put(structureFeature, i);
                                }
                            }
                        }
                    }

                    return object2IntMap;
                }
            }
        }
    }

    private static Object2IntMap<StructureGenerator<?>> deduplicateEmptyMap(Object2IntMap<StructureGenerator<?>> map) {
        return map.isEmpty() ? Object2IntMaps.emptyMap() : map;
    }

    private StructureCheckResult checkStructureInfo(Object2IntMap<StructureGenerator<?>> referencesByStructure, StructureGenerator<?> feature, boolean skipExistingChunk) {
        int i = referencesByStructure.getOrDefault(feature, -1);
        return i == -1 || skipExistingChunk && i != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
    }

    public void onStructureLoad(ChunkCoordIntPair pos, Map<StructureGenerator<?>, StructureStart<?>> structureStarts) {
        long l = pos.pair();
        Object2IntMap<StructureGenerator<?>> object2IntMap = new Object2IntOpenHashMap<>();
        structureStarts.forEach((start, structureStart) -> {
            if (structureStart.isValid()) {
                object2IntMap.put(start, structureStart.getReferences());
            }

        });
        this.storeFullResults(l, object2IntMap);
    }

    private void storeFullResults(long pos, Object2IntMap<StructureGenerator<?>> referencesByStructure) {
        this.loadedChunks.put(pos, deduplicateEmptyMap(referencesByStructure));
        this.featureChecks.values().forEach((generationPossibilityByChunkPos) -> {
            generationPossibilityByChunkPos.remove(pos);
        });
    }

    public void incrementReference(ChunkCoordIntPair pos, StructureGenerator<?> feature) {
        this.loadedChunks.compute(pos.pair(), (posx, referencesByStructure) -> {
            if (referencesByStructure == null || referencesByStructure.isEmpty()) {
                referencesByStructure = new Object2IntOpenHashMap<>();
            }

            referencesByStructure.computeInt(feature, (featurex, references) -> {
                return references == null ? 1 : references + 1;
            });
            return referencesByStructure;
        });
    }
}
