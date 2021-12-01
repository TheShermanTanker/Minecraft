package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.data.worldgen.WorldGenStructureFeatures;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.BiomeSource$StepFeatureData;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.ChunkProviderDebug;
import net.minecraft.world.level.levelgen.ChunkProviderFlat;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsStronghold;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public abstract class ChunkGenerator implements BiomeManager.Provider {
    public static final Codec<ChunkGenerator> CODEC = IRegistry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
    protected final WorldChunkManager biomeSource;
    protected final WorldChunkManager runtimeBiomeSource;
    private final StructureSettings settings;
    public final long strongholdSeed;
    private final List<ChunkCoordIntPair> strongholdPositions = Lists.newArrayList();

    public ChunkGenerator(WorldChunkManager biomeSource, StructureSettings structuresConfig) {
        this(biomeSource, biomeSource, structuresConfig, 0L);
    }

    public ChunkGenerator(WorldChunkManager populationSource, WorldChunkManager biomeSource, StructureSettings structuresConfig, long worldSeed) {
        this.biomeSource = populationSource;
        this.runtimeBiomeSource = biomeSource;
        this.settings = structuresConfig;
        this.strongholdSeed = worldSeed;
    }

    private void generateStrongholds() {
        if (this.strongholdPositions.isEmpty()) {
            StructureSettingsStronghold strongholdConfiguration = this.settings.stronghold();
            if (strongholdConfiguration != null && strongholdConfiguration.count() != 0) {
                List<BiomeBase> list = Lists.newArrayList();

                for(BiomeBase biome : this.biomeSource.possibleBiomes()) {
                    if (validStrongholdBiome(biome)) {
                        list.add(biome);
                    }
                }

                int i = strongholdConfiguration.distance();
                int j = strongholdConfiguration.count();
                int k = strongholdConfiguration.spread();
                Random random = new Random();
                random.setSeed(this.strongholdSeed);
                double d = random.nextDouble() * Math.PI * 2.0D;
                int l = 0;
                int m = 0;

                for(int n = 0; n < j; ++n) {
                    double e = (double)(4 * i + i * m * 6) + (random.nextDouble() - 0.5D) * (double)i * 2.5D;
                    int o = (int)Math.round(Math.cos(d) * e);
                    int p = (int)Math.round(Math.sin(d) * e);
                    BlockPosition blockPos = this.biomeSource.findBiomeHorizontal(SectionPosition.sectionToBlockCoord(o, 8), 0, SectionPosition.sectionToBlockCoord(p, 8), 112, list::contains, random, this.climateSampler());
                    if (blockPos != null) {
                        o = SectionPosition.blockToSectionCoord(blockPos.getX());
                        p = SectionPosition.blockToSectionCoord(blockPos.getZ());
                    }

                    this.strongholdPositions.add(new ChunkCoordIntPair(o, p));
                    d += (Math.PI * 2D) / (double)k;
                    ++l;
                    if (l == k) {
                        ++m;
                        l = 0;
                        k = k + 2 * k / (m + 1);
                        k = Math.min(k, j - n);
                        d += random.nextDouble() * Math.PI * 2.0D;
                    }
                }

            }
        }
    }

    private static boolean validStrongholdBiome(BiomeBase biome) {
        BiomeBase.Geography biomeCategory = biome.getBiomeCategory();
        return biomeCategory != BiomeBase.Geography.OCEAN && biomeCategory != BiomeBase.Geography.RIVER && biomeCategory != BiomeBase.Geography.BEACH && biomeCategory != BiomeBase.Geography.SWAMP && biomeCategory != BiomeBase.Geography.NETHER && biomeCategory != BiomeBase.Geography.THEEND;
    }

    protected abstract Codec<? extends ChunkGenerator> codec();

    public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
        return IRegistry.CHUNK_GENERATOR.getResourceKey(this.codec());
    }

    public abstract ChunkGenerator withSeed(long seed);

    public CompletableFuture<IChunkAccess> createBiomes(IRegistry<BiomeBase> biomeRegistry, Executor executor, Blender blender, StructureManager structureAccessor, IChunkAccess chunk) {
        return CompletableFuture.supplyAsync(SystemUtils.wrapThreadWithTaskName("init_biomes", () -> {
            chunk.fillBiomesFromNoise(this.runtimeBiomeSource::getNoiseBiome, this.climateSampler());
            return chunk;
        }), SystemUtils.backgroundExecutor());
    }

    public abstract Climate.Sampler climateSampler();

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        return this.getWorldChunkManager().getNoiseBiome(biomeX, biomeY, biomeZ, this.climateSampler());
    }

    public abstract void applyCarvers(RegionLimitedWorldAccess chunkRegion, long seed, BiomeManager biomeAccess, StructureManager structureAccessor, IChunkAccess chunk, WorldGenStage.Features generationStep);

    @Nullable
    public BlockPosition findNearestMapFeature(WorldServer world, StructureGenerator<?> structureFeature, BlockPosition center, int radius, boolean skipExistingChunks) {
        if (structureFeature == StructureGenerator.STRONGHOLD) {
            this.generateStrongholds();
            BlockPosition blockPos = null;
            double d = Double.MAX_VALUE;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(ChunkCoordIntPair chunkPos : this.strongholdPositions) {
                mutableBlockPos.set(SectionPosition.sectionToBlockCoord(chunkPos.x, 8), 32, SectionPosition.sectionToBlockCoord(chunkPos.z, 8));
                double e = mutableBlockPos.distSqr(center);
                if (blockPos == null) {
                    blockPos = new BlockPosition(mutableBlockPos);
                    d = e;
                } else if (e < d) {
                    blockPos = new BlockPosition(mutableBlockPos);
                    d = e;
                }
            }

            return blockPos;
        } else {
            StructureSettingsFeature structureFeatureConfiguration = this.settings.getConfig(structureFeature);
            ImmutableMultimap<StructureFeature<?, ?>, ResourceKey<BiomeBase>> immutableMultimap = this.settings.structures(structureFeature);
            if (structureFeatureConfiguration != null && !immutableMultimap.isEmpty()) {
                IRegistry<BiomeBase> registry = world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY);
                Set<ResourceKey<BiomeBase>> set = this.runtimeBiomeSource.possibleBiomes().stream().flatMap((biome) -> {
                    return registry.getResourceKey(biome).stream();
                }).collect(Collectors.toSet());
                return immutableMultimap.values().stream().noneMatch(set::contains) ? null : structureFeature.getNearestGeneratedFeature(world, world.getStructureManager(), center, radius, skipExistingChunks, world.getSeed(), structureFeatureConfiguration);
            } else {
                return null;
            }
        }
    }

    public void applyBiomeDecoration(GeneratorAccessSeed world, IChunkAccess chunk, StructureManager structureAccessor) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        if (!SharedConstants.debugVoidTerrain(chunkPos)) {
            SectionPosition sectionPos = SectionPosition.of(chunkPos, world.getMinSection());
            BlockPosition blockPos = sectionPos.origin();
            Map<Integer, List<StructureGenerator<?>>> map = IRegistry.STRUCTURE_FEATURE.stream().collect(Collectors.groupingBy((structureFeature) -> {
                return structureFeature.step().ordinal();
            }));
            List<BiomeSource$StepFeatureData> list = this.biomeSource.featuresPerStep();
            SeededRandom worldgenRandom = new SeededRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
            long l = worldgenRandom.setDecorationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
            Set<BiomeBase> set = new ObjectArraySet<>();
            if (this instanceof ChunkProviderFlat) {
                set.addAll(this.biomeSource.possibleBiomes());
            } else {
                ChunkCoordIntPair.rangeClosed(sectionPos.chunk(), 1).forEach((chunkPosx) -> {
                    IChunkAccess chunkAccess = world.getChunkAt(chunkPosx.x, chunkPosx.z);

                    for(ChunkSection levelChunkSection : chunkAccess.getSections()) {
                        levelChunkSection.getBiomes().getAll(set::add);
                    }

                });
                set.retainAll(this.biomeSource.possibleBiomes());
            }

            int i = list.size();

            try {
                IRegistry<PlacedFeature> registry = world.registryAccess().registryOrThrow(IRegistry.PLACED_FEATURE_REGISTRY);
                IRegistry<StructureGenerator<?>> registry2 = world.registryAccess().registryOrThrow(IRegistry.STRUCTURE_FEATURE_REGISTRY);
                int j = Math.max(WorldGenStage.Decoration.values().length, i);

                for(int k = 0; k < j; ++k) {
                    int m = 0;
                    if (structureAccessor.shouldGenerateFeatures()) {
                        for(StructureGenerator<?> structureFeature : map.getOrDefault(k, Collections.emptyList())) {
                            worldgenRandom.setFeatureSeed(l, m, k);
                            Supplier<String> supplier = () -> {
                                return registry2.getResourceKey(structureFeature).map(Object::toString).orElseGet(structureFeature::toString);
                            };

                            try {
                                world.setCurrentlyGenerating(supplier);
                                structureAccessor.startsForFeature(sectionPos, structureFeature).forEach((structureStart) -> {
                                    structureStart.placeInChunk(world, structureAccessor, this, worldgenRandom, getWritableArea(chunk), chunkPos);
                                });
                            } catch (Exception var29) {
                                CrashReport crashReport = CrashReport.forThrowable(var29, "Feature placement");
                                crashReport.addCategory("Feature").setDetail("Description", supplier::get);
                                throw new ReportedException(crashReport);
                            }

                            ++m;
                        }
                    }

                    if (k < i) {
                        IntSet intSet = new IntArraySet();

                        for(BiomeBase biome : set) {
                            List<List<Supplier<PlacedFeature>>> list3 = biome.getGenerationSettings().features();
                            if (k < list3.size()) {
                                List<Supplier<PlacedFeature>> list4 = list3.get(k);
                                BiomeSource$StepFeatureData stepFeatureData = list.get(k);
                                list4.stream().map(Supplier::get).forEach((placedFeaturex) -> {
                                    intSet.add(stepFeatureData.indexMapping().applyAsInt(placedFeaturex));
                                });
                            }
                        }

                        int n = intSet.size();
                        int[] is = intSet.toIntArray();
                        Arrays.sort(is);
                        BiomeSource$StepFeatureData stepFeatureData2 = list.get(k);

                        for(int o = 0; o < n; ++o) {
                            int p = is[o];
                            PlacedFeature placedFeature = stepFeatureData2.features().get(p);
                            Supplier<String> supplier2 = () -> {
                                return registry.getResourceKey(placedFeature).map(Object::toString).orElseGet(placedFeature::toString);
                            };
                            worldgenRandom.setFeatureSeed(l, p, k);

                            try {
                                world.setCurrentlyGenerating(supplier2);
                                placedFeature.placeWithBiomeCheck(world, this, worldgenRandom, blockPos);
                            } catch (Exception var30) {
                                CrashReport crashReport2 = CrashReport.forThrowable(var30, "Feature placement");
                                crashReport2.addCategory("Feature").setDetail("Description", supplier2::get);
                                throw new ReportedException(crashReport2);
                            }
                        }
                    }
                }

                world.setCurrentlyGenerating((Supplier<String>)null);
            } catch (Exception var31) {
                CrashReport crashReport3 = CrashReport.forThrowable(var31, "Biome decoration");
                crashReport3.addCategory("Generation").setDetail("CenterX", chunkPos.x).setDetail("CenterZ", chunkPos.z).setDetail("Seed", l);
                throw new ReportedException(crashReport3);
            }
        }
    }

    private static StructureBoundingBox getWritableArea(IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        IWorldHeightAccess levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int k = levelHeightAccessor.getMinBuildHeight() + 1;
        int l = levelHeightAccessor.getMaxBuildHeight() - 1;
        return new StructureBoundingBox(i, k, j, i + 15, l, j + 15);
    }

    public abstract void buildSurface(RegionLimitedWorldAccess region, StructureManager structures, IChunkAccess chunk);

    public abstract void addMobs(RegionLimitedWorldAccess region);

    public StructureSettings getSettings() {
        return this.settings;
    }

    public int getSpawnHeight(IWorldHeightAccess world) {
        return 64;
    }

    public WorldChunkManager getWorldChunkManager() {
        return this.runtimeBiomeSource;
    }

    public abstract int getGenerationDepth();

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getMobsFor(BiomeBase biome, StructureManager accessor, EnumCreatureType group, BlockPosition pos) {
        return biome.getMobSettings().getMobs(group);
    }

    public void createStructures(IRegistryCustom registryManager, StructureManager structureAccessor, IChunkAccess chunk, DefinedStructureManager structureManager, long worldSeed) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        SectionPosition sectionPos = SectionPosition.bottomOf(chunk);
        StructureSettingsFeature structureFeatureConfiguration = this.settings.getConfig(StructureGenerator.STRONGHOLD);
        if (structureFeatureConfiguration != null) {
            StructureStart<?> structureStart = structureAccessor.getStartForFeature(sectionPos, StructureGenerator.STRONGHOLD, chunk);
            if (structureStart == null || !structureStart.isValid()) {
                StructureStart<?> structureStart2 = WorldGenStructureFeatures.STRONGHOLD.generate(registryManager, this, this.biomeSource, structureManager, worldSeed, chunkPos, fetchReferences(structureAccessor, chunk, sectionPos, StructureGenerator.STRONGHOLD), structureFeatureConfiguration, chunk, ChunkGenerator::validStrongholdBiome);
                structureAccessor.setStartForFeature(sectionPos, StructureGenerator.STRONGHOLD, structureStart2, chunk);
            }
        }

        IRegistry<BiomeBase> registry = registryManager.registryOrThrow(IRegistry.BIOME_REGISTRY);

        label48:
        for(StructureGenerator<?> structureFeature : IRegistry.STRUCTURE_FEATURE) {
            if (structureFeature != StructureGenerator.STRONGHOLD) {
                StructureSettingsFeature structureFeatureConfiguration2 = this.settings.getConfig(structureFeature);
                if (structureFeatureConfiguration2 != null) {
                    StructureStart<?> structureStart3 = structureAccessor.getStartForFeature(sectionPos, structureFeature, chunk);
                    if (structureStart3 == null || !structureStart3.isValid()) {
                        int i = fetchReferences(structureAccessor, chunk, sectionPos, structureFeature);

                        for(Entry<StructureFeature<?, ?>, Collection<ResourceKey<BiomeBase>>> entry : this.settings.structures(structureFeature).asMap().entrySet()) {
                            StructureStart<?> structureStart4 = entry.getKey().generate(registryManager, this, this.biomeSource, structureManager, worldSeed, chunkPos, i, structureFeatureConfiguration2, chunk, (b) -> {
                                return this.validBiome(registry, entry.getValue()::contains, b);
                            });
                            if (structureStart4.isValid()) {
                                structureAccessor.setStartForFeature(sectionPos, structureFeature, structureStart4, chunk);
                                continue label48;
                            }
                        }

                        structureAccessor.setStartForFeature(sectionPos, structureFeature, StructureStart.INVALID_START, chunk);
                    }
                }
            }
        }

    }

    private static int fetchReferences(StructureManager structureAccessor, IChunkAccess chunk, SectionPosition sectionPos, StructureGenerator<?> structureFeature) {
        StructureStart<?> structureStart = structureAccessor.getStartForFeature(sectionPos, structureFeature, chunk);
        return structureStart != null ? structureStart.getReferences() : 0;
    }

    protected boolean validBiome(IRegistry<BiomeBase> registry, Predicate<ResourceKey<BiomeBase>> condition, BiomeBase biome) {
        return registry.getResourceKey(biome).filter(condition).isPresent();
    }

    public void storeStructures(GeneratorAccessSeed world, StructureManager structureAccessor, IChunkAccess chunk) {
        int i = 8;
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
        int l = chunkPos.getMinBlockX();
        int m = chunkPos.getMinBlockZ();
        SectionPosition sectionPos = SectionPosition.bottomOf(chunk);

        for(int n = j - 8; n <= j + 8; ++n) {
            for(int o = k - 8; o <= k + 8; ++o) {
                long p = ChunkCoordIntPair.pair(n, o);

                for(StructureStart<?> structureStart : world.getChunkAt(n, o).getAllStarts().values()) {
                    try {
                        if (structureStart.isValid() && structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
                            structureAccessor.addReferenceForFeature(sectionPos, structureStart.getFeature(), p, chunk);
                            PacketDebug.sendStructurePacket(world, structureStart);
                        }
                    } catch (Exception var20) {
                        CrashReport crashReport = CrashReport.forThrowable(var20, "Generating structure reference");
                        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Structure");
                        crashReportCategory.setDetail("Id", () -> {
                            return IRegistry.STRUCTURE_FEATURE.getKey(structureStart.getFeature()).toString();
                        });
                        crashReportCategory.setDetail("Name", () -> {
                            return structureStart.getFeature().getFeatureName();
                        });
                        crashReportCategory.setDetail("Class", () -> {
                            return structureStart.getFeature().getClass().getCanonicalName();
                        });
                        throw new ReportedException(crashReport);
                    }
                }
            }
        }

    }

    public abstract CompletableFuture<IChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureManager structureAccessor, IChunkAccess chunk);

    public abstract int getSeaLevel();

    public abstract int getMinY();

    public abstract int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world);

    public abstract net.minecraft.world.level.BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world);

    public int getFirstFreeHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        return this.getBaseHeight(x, z, heightmap, world);
    }

    public int getFirstOccupiedHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        return this.getBaseHeight(x, z, heightmap, world) - 1;
    }

    public boolean hasStronghold(ChunkCoordIntPair pos) {
        this.generateStrongholds();
        return this.strongholdPositions.contains(pos);
    }

    static {
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "noise", ChunkGeneratorAbstract.CODEC);
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "flat", ChunkProviderFlat.CODEC);
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "debug", ChunkProviderDebug.CODEC);
    }
}
