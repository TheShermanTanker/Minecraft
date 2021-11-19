package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPosition;
import net.minecraft.data.worldgen.WorldGenStructureFeatures;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.ChunkProviderDebug;
import net.minecraft.world.level.levelgen.ChunkProviderFlat;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.SingleBaseStoneSource;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsStronghold;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public abstract class ChunkGenerator {
    public static final Codec<ChunkGenerator> CODEC = IRegistry.CHUNK_GENERATOR.dispatchStable(ChunkGenerator::codec, Function.identity());
    protected final WorldChunkManager biomeSource;
    protected final WorldChunkManager runtimeBiomeSource;
    private final StructureSettings settings;
    private final long strongholdSeed;
    private final List<ChunkCoordIntPair> strongholdPositions = Lists.newArrayList();
    private final BaseStoneSource defaultBaseStoneSource;

    public ChunkGenerator(WorldChunkManager biomeSource, StructureSettings structuresConfig) {
        this(biomeSource, biomeSource, structuresConfig, 0L);
    }

    public ChunkGenerator(WorldChunkManager populationSource, WorldChunkManager biomeSource, StructureSettings structuresConfig, long worldSeed) {
        this.biomeSource = populationSource;
        this.runtimeBiomeSource = biomeSource;
        this.settings = structuresConfig;
        this.strongholdSeed = worldSeed;
        this.defaultBaseStoneSource = new SingleBaseStoneSource(Blocks.STONE.getBlockData());
    }

    private void generateStrongholds() {
        if (this.strongholdPositions.isEmpty()) {
            StructureSettingsStronghold strongholdConfiguration = this.settings.stronghold();
            if (strongholdConfiguration != null && strongholdConfiguration.count() != 0) {
                List<BiomeBase> list = Lists.newArrayList();

                for(BiomeBase biome : this.biomeSource.possibleBiomes()) {
                    if (biome.getGenerationSettings().isValidStart(StructureGenerator.STRONGHOLD)) {
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
                    BlockPosition blockPos = this.biomeSource.findBiomeHorizontal(SectionPosition.sectionToBlockCoord(o, 8), 0, SectionPosition.sectionToBlockCoord(p, 8), 112, list::contains, random);
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

    protected abstract Codec<? extends ChunkGenerator> codec();

    public abstract ChunkGenerator withSeed(long seed);

    public void createBiomes(IRegistry<BiomeBase> biomeRegistry, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        ((ProtoChunk)chunk).setBiomes(new BiomeStorage(biomeRegistry, chunk, chunkPos, this.runtimeBiomeSource));
    }

    public void doCarving(long seed, BiomeManager access, IChunkAccess chunk, WorldGenStage.Features carver) {
        BiomeManager biomeManager = access.withDifferentSource(this.biomeSource);
        SeededRandom worldgenRandom = new SeededRandom();
        int i = 8;
        ChunkCoordIntPair chunkPos = chunk.getPos();
        CarvingContext carvingContext = new CarvingContext(this, chunk);
        Aquifer aquifer = this.createAquifer(chunk);
        BitSet bitSet = ((ProtoChunk)chunk).getOrCreateCarvingMask(carver);

        for(int j = -8; j <= 8; ++j) {
            for(int k = -8; k <= 8; ++k) {
                ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(chunkPos.x + j, chunkPos.z + k);
                BiomeSettingsGeneration biomeGenerationSettings = this.biomeSource.getBiome(QuartPos.fromBlock(chunkPos2.getMinBlockX()), 0, QuartPos.fromBlock(chunkPos2.getMinBlockZ())).getGenerationSettings();
                List<Supplier<WorldGenCarverWrapper<?>>> list = biomeGenerationSettings.getCarvers(carver);
                ListIterator<Supplier<WorldGenCarverWrapper<?>>> listIterator = list.listIterator();

                while(listIterator.hasNext()) {
                    int l = listIterator.nextIndex();
                    WorldGenCarverWrapper<?> configuredWorldCarver = listIterator.next().get();
                    worldgenRandom.setLargeFeatureSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
                    if (configuredWorldCarver.isStartChunk(worldgenRandom)) {
                        configuredWorldCarver.carve(carvingContext, chunk, biomeManager::getBiome, worldgenRandom, aquifer, chunkPos2, bitSet);
                    }
                }
            }
        }

    }

    protected Aquifer createAquifer(IChunkAccess chunk) {
        return Aquifer.createDisabled(this.getSeaLevel(), Blocks.WATER.getBlockData());
    }

    @Nullable
    public BlockPosition findNearestMapFeature(WorldServer world, StructureGenerator<?> feature, BlockPosition center, int radius, boolean skipExistingChunks) {
        if (!this.biomeSource.canGenerateStructure(feature)) {
            return null;
        } else if (feature == StructureGenerator.STRONGHOLD) {
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
            StructureSettingsFeature structureFeatureConfiguration = this.settings.getConfig(feature);
            return structureFeatureConfiguration == null ? null : feature.getNearestGeneratedFeature(world, world.getStructureManager(), center, radius, skipExistingChunks, world.getSeed(), structureFeatureConfiguration);
        }
    }

    public void addDecorations(RegionLimitedWorldAccess region, StructureManager accessor) {
        ChunkCoordIntPair chunkPos = region.getCenter();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        BlockPosition blockPos = new BlockPosition(i, region.getMinBuildHeight(), j);
        BiomeBase biome = this.biomeSource.getPrimaryBiome(chunkPos);
        SeededRandom worldgenRandom = new SeededRandom();
        long l = worldgenRandom.setDecorationSeed(region.getSeed(), i, j);

        try {
            biome.generate(accessor, this, region, l, worldgenRandom, blockPos);
        } catch (Exception var13) {
            CrashReport crashReport = CrashReport.forThrowable(var13, "Biome decoration");
            crashReport.addCategory("Generation").setDetail("CenterX", chunkPos.x).setDetail("CenterZ", chunkPos.z).setDetail("Seed", l).setDetail("Biome", biome);
            throw new ReportedException(crashReport);
        }
    }

    public abstract void buildBase(RegionLimitedWorldAccess region, IChunkAccess chunk);

    public void addMobs(RegionLimitedWorldAccess region) {
    }

    public StructureSettings getSettings() {
        return this.settings;
    }

    public int getSpawnHeight(IWorldHeightAccess world) {
        return 64;
    }

    public WorldChunkManager getWorldChunkManager() {
        return this.runtimeBiomeSource;
    }

    public int getGenerationDepth() {
        return 256;
    }

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getMobsFor(BiomeBase biome, StructureManager accessor, EnumCreatureType group, BlockPosition pos) {
        return biome.getMobSettings().getMobs(group);
    }

    public void createStructures(IRegistryCustom registryManager, StructureManager accessor, IChunkAccess chunk, DefinedStructureManager structureManager, long worldSeed) {
        BiomeBase biome = this.biomeSource.getPrimaryBiome(chunk.getPos());
        this.createStructure(WorldGenStructureFeatures.STRONGHOLD, registryManager, accessor, chunk, structureManager, worldSeed, biome);

        for(Supplier<StructureFeature<?, ?>> supplier : biome.getGenerationSettings().structures()) {
            this.createStructure(supplier.get(), registryManager, accessor, chunk, structureManager, worldSeed, biome);
        }

    }

    private void createStructure(StructureFeature<?, ?> feature, IRegistryCustom registryManager, StructureManager accessor, IChunkAccess chunk, DefinedStructureManager structureManager, long worldSeed, BiomeBase biome) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        SectionPosition sectionPos = SectionPosition.bottomOf(chunk);
        StructureStart<?> structureStart = accessor.getStartForFeature(sectionPos, feature.feature, chunk);
        int i = structureStart != null ? structureStart.getReferences() : 0;
        StructureSettingsFeature structureFeatureConfiguration = this.settings.getConfig(feature.feature);
        if (structureFeatureConfiguration != null) {
            StructureStart<?> structureStart2 = feature.generate(registryManager, this, this.biomeSource, structureManager, worldSeed, chunkPos, biome, i, structureFeatureConfiguration, chunk);
            accessor.setStartForFeature(sectionPos, feature.feature, structureStart2, chunk);
        }

    }

    public void storeStructures(GeneratorAccessSeed world, StructureManager accessor, IChunkAccess chunk) {
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
                            accessor.addReferenceForFeature(sectionPos, structureStart.getFeature(), p, chunk);
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

    public abstract CompletableFuture<IChunkAccess> buildNoise(Executor executor, StructureManager accessor, IChunkAccess chunk);

    public int getSeaLevel() {
        return 63;
    }

    public int getMinY() {
        return 0;
    }

    public abstract int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world);

    public abstract BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world);

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

    public BaseStoneSource getBaseStoneSource() {
        return this.defaultBaseStoneSource;
    }

    static {
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "noise", ChunkGeneratorAbstract.CODEC);
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "flat", ChunkProviderFlat.CODEC);
        IRegistry.register(IRegistry.CHUNK_GENERATOR, "debug", ChunkProviderDebug.CODEC);
    }
}
