package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.util.MathHelper;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeaturePillagerOutpost;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureSwampHut;
import net.minecraft.world.level.levelgen.feature.WorldGenMonument;
import net.minecraft.world.level.levelgen.feature.WorldGenNether;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;
import net.minecraft.world.level.levelgen.material.WorldGenMaterialRule;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public final class ChunkGeneratorAbstract extends ChunkGenerator {
    public static final Codec<ChunkGeneratorAbstract> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryLookupCodec.create(IRegistry.NOISE_REGISTRY).forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.noises;
        }), WorldChunkManager.CODEC.fieldOf("biome_source").forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.biomeSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.seed;
        }), GeneratorSettingBase.CODEC.fieldOf("settings").forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.settings;
        })).apply(instance, instance.stable(ChunkGeneratorAbstract::new));
    });
    private static final IBlockData AIR = Blocks.AIR.getBlockData();
    private static final IBlockData[] EMPTY_COLUMN = new IBlockData[0];
    protected final IBlockData defaultBlock;
    public final IRegistry<NormalNoise$NoiseParameters> noises;
    private final long seed;
    public final Supplier<GeneratorSettingBase> settings;
    private final NoiseSampler sampler;
    private final SurfaceSystem surfaceSystem;
    private final WorldGenMaterialRule materialRule;
    private final Aquifer.FluidPicker globalFluidPicker;

    public ChunkGeneratorAbstract(IRegistry<NormalNoise$NoiseParameters> noiseRegistry, WorldChunkManager biomeSource, long seed, Supplier<GeneratorSettingBase> settings) {
        this(noiseRegistry, biomeSource, biomeSource, seed, settings);
    }

    private ChunkGeneratorAbstract(IRegistry<NormalNoise$NoiseParameters> noiseRegistry, WorldChunkManager populationSource, WorldChunkManager biomeSource, long seed, Supplier<GeneratorSettingBase> settings) {
        super(populationSource, biomeSource, settings.get().structureSettings(), seed);
        this.noises = noiseRegistry;
        this.seed = seed;
        this.settings = settings;
        GeneratorSettingBase noiseGeneratorSettings = this.settings.get();
        this.defaultBlock = noiseGeneratorSettings.getDefaultBlock();
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        this.sampler = new NoiseSampler(noiseSettings, noiseGeneratorSettings.isNoiseCavesEnabled(), seed, noiseRegistry, noiseGeneratorSettings.getRandomSource());
        Builder<WorldGenMaterialRule> builder = ImmutableList.builder();
        builder.add(NoiseChunk::updateNoiseAndGenerateBaseState);
        builder.add(NoiseChunk::oreVeinify);
        this.materialRule = new MaterialRuleList(builder.build());
        Aquifer.FluidStatus fluidStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.getBlockData());
        int i = noiseGeneratorSettings.seaLevel();
        Aquifer.FluidStatus fluidStatus2 = new Aquifer.FluidStatus(i, noiseGeneratorSettings.getDefaultFluid());
        Aquifer.FluidStatus fluidStatus3 = new Aquifer.FluidStatus(noiseSettings.minY() - 1, Blocks.AIR.getBlockData());
        this.globalFluidPicker = (j, k, l) -> {
            return k < Math.min(-54, i) ? fluidStatus : fluidStatus2;
        };
        this.surfaceSystem = new SurfaceSystem(noiseRegistry, this.defaultBlock, i, seed, noiseGeneratorSettings.getRandomSource());
    }

    @Override
    public CompletableFuture<IChunkAccess> createBiomes(IRegistry<BiomeBase> biomeRegistry, Executor executor, Blender blender, StructureManager structureAccessor, IChunkAccess chunk) {
        return CompletableFuture.supplyAsync(SystemUtils.wrapThreadWithTaskName("init_biomes", () -> {
            this.doCreateBiomes(biomeRegistry, blender, structureAccessor, chunk);
            return chunk;
        }), SystemUtils.backgroundExecutor());
    }

    private void doCreateBiomes(IRegistry<BiomeBase> biomeRegistry, Blender blender, StructureManager structureAccessor, IChunkAccess chunk) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.sampler, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, this.settings.get(), this.globalFluidPicker, blender);
        BiomeResolver biomeResolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.runtimeBiomeSource), biomeRegistry, chunk);
        chunk.fillBiomesFromNoise(biomeResolver, (x, y, z) -> {
            return this.sampler.target(x, y, z, noiseChunk.noiseData(x, z));
        });
    }

    @Override
    public Climate.Sampler climateSampler() {
        return this.sampler;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new ChunkGeneratorAbstract(this.noises, this.biomeSource.withSeed(seed), seed, this.settings);
    }

    public boolean stable(long seed, ResourceKey<GeneratorSettingBase> settingsKey) {
        return this.seed == seed && this.settings.get().stable(settingsKey);
    }

    @Override
    public int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        int i = Math.max(noiseSettings.minY(), world.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), world.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = MathHelper.intFloorDiv(j - i, noiseSettings.getCellHeight());
        return l <= 0 ? world.getMinBuildHeight() : this.iterateNoiseColumn(x, z, (IBlockData[])null, heightmap.isOpaque(), k, l).orElse(world.getMinBuildHeight());
    }

    @Override
    public BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        int i = Math.max(noiseSettings.minY(), world.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), world.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = MathHelper.intFloorDiv(j - i, noiseSettings.getCellHeight());
        if (l <= 0) {
            return new BlockColumn(i, EMPTY_COLUMN);
        } else {
            IBlockData[] blockStates = new IBlockData[l * noiseSettings.getCellHeight()];
            this.iterateNoiseColumn(x, z, blockStates, (Predicate<IBlockData>)null, k, l);
            return new BlockColumn(i, blockStates);
        }
    }

    private OptionalInt iterateNoiseColumn(int i, int j, @Nullable IBlockData[] states, @Nullable Predicate<IBlockData> predicate, int k, int l) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        int m = noiseSettings.getCellWidth();
        int n = noiseSettings.getCellHeight();
        int o = Math.floorDiv(i, m);
        int p = Math.floorDiv(j, m);
        int q = Math.floorMod(i, m);
        int r = Math.floorMod(j, m);
        int s = o * m;
        int t = p * m;
        double d = (double)q / (double)m;
        double e = (double)r / (double)m;
        NoiseChunk noiseChunk = NoiseChunk.forColumn(s, t, k, l, this.sampler, this.settings.get(), this.globalFluidPicker);
        noiseChunk.initializeForFirstCellX();
        noiseChunk.advanceCellX(0);

        for(int u = l - 1; u >= 0; --u) {
            noiseChunk.selectCellYZ(u, 0);

            for(int v = n - 1; v >= 0; --v) {
                int w = (k + u) * n + v;
                double f = (double)v / (double)n;
                noiseChunk.updateForY(f);
                noiseChunk.updateForX(d);
                noiseChunk.updateForZ(e);
                IBlockData blockState = this.materialRule.apply(noiseChunk, i, w, j);
                IBlockData blockState2 = blockState == null ? this.defaultBlock : blockState;
                if (states != null) {
                    int x = u * n + v;
                    states[x] = blockState2;
                }

                if (predicate != null && predicate.test(blockState2)) {
                    return OptionalInt.of(w + 1);
                }
            }
        }

        return OptionalInt.empty();
    }

    @Override
    public void buildSurface(RegionLimitedWorldAccess region, StructureManager structures, IChunkAccess chunk) {
        if (!SharedConstants.debugVoidTerrain(chunk.getPos())) {
            WorldGenerationContext worldGenerationContext = new WorldGenerationContext(this, region);
            GeneratorSettingBase noiseGeneratorSettings = this.settings.get();
            NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.sampler, () -> {
                return new Beardifier(structures, chunk);
            }, noiseGeneratorSettings, this.globalFluidPicker, Blender.of(region));
            this.surfaceSystem.buildSurface(region.getBiomeManager(), region.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), noiseGeneratorSettings.useLegacyRandomSource(), worldGenerationContext, chunk, noiseChunk, noiseGeneratorSettings.surfaceRule());
        }
    }

    @Override
    public void applyCarvers(RegionLimitedWorldAccess chunkRegion, long seed, BiomeManager biomeAccess, StructureManager structureAccessor, IChunkAccess chunk, WorldGenStage.Features generationStep) {
        BiomeManager biomeManager = biomeAccess.withDifferentSource((x, y, z) -> {
            return this.biomeSource.getNoiseBiome(x, y, z, this.climateSampler());
        });
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
        int i = 8;
        ChunkCoordIntPair chunkPos = chunk.getPos();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.sampler, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, this.settings.get(), this.globalFluidPicker, Blender.of(chunkRegion));
        Aquifer aquifer = noiseChunk.aquifer();
        CarvingContext carvingContext = new CarvingContext(this, chunkRegion.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk);
        CarvingMask carvingMask = ((ProtoChunk)chunk).getOrCreateCarvingMask(generationStep);

        for(int j = -8; j <= 8; ++j) {
            for(int k = -8; k <= 8; ++k) {
                ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(chunkPos.x + j, chunkPos.z + k);
                IChunkAccess chunkAccess = chunkRegion.getChunkAt(chunkPos2.x, chunkPos2.z);
                BiomeSettingsGeneration biomeGenerationSettings = chunkAccess.carverBiome(() -> {
                    return this.biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkPos2.getMinBlockX()), 0, QuartPos.fromBlock(chunkPos2.getMinBlockZ()), this.climateSampler());
                }).getGenerationSettings();
                List<Supplier<WorldGenCarverWrapper<?>>> list = biomeGenerationSettings.getCarvers(generationStep);
                ListIterator<Supplier<WorldGenCarverWrapper<?>>> listIterator = list.listIterator();

                while(listIterator.hasNext()) {
                    int l = listIterator.nextIndex();
                    WorldGenCarverWrapper<?> configuredWorldCarver = listIterator.next().get();
                    worldgenRandom.setLargeFeatureSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
                    if (configuredWorldCarver.isStartChunk(worldgenRandom)) {
                        configuredWorldCarver.carve(carvingContext, chunk, biomeManager::getBiome, worldgenRandom, aquifer, chunkPos2, carvingMask);
                    }
                }
            }
        }

    }

    @Override
    public CompletableFuture<IChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureManager structureAccessor, IChunkAccess chunk) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        IWorldHeightAccess levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int i = Math.max(noiseSettings.minY(), levelHeightAccessor.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), levelHeightAccessor.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = MathHelper.intFloorDiv(j - i, noiseSettings.getCellHeight());
        if (l <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int m = chunk.getSectionIndex(l * noiseSettings.getCellHeight() - 1 + i);
            int n = chunk.getSectionIndex(i);
            Set<ChunkSection> set = Sets.newHashSet();

            for(int o = m; o >= n; --o) {
                ChunkSection levelChunkSection = chunk.getSection(o);
                levelChunkSection.acquire();
                set.add(levelChunkSection);
            }

            return CompletableFuture.supplyAsync(SystemUtils.wrapThreadWithTaskName("wgen_fill_noise", () -> {
                return this.doFill(blender, structureAccessor, chunk, k, l);
            }), SystemUtils.backgroundExecutor()).whenCompleteAsync((chunkAccess, throwable) -> {
                for(ChunkSection levelChunkSection : set) {
                    levelChunkSection.release();
                }

            }, executor);
        }
    }

    private IChunkAccess doFill(Blender blender, StructureManager structureAccessor, IChunkAccess chunk, int i, int j) {
        GeneratorSettingBase noiseGeneratorSettings = this.settings.get();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.sampler, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, noiseGeneratorSettings, this.globalFluidPicker, blender);
        HeightMap heightmap = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.OCEAN_FLOOR_WG);
        HeightMap heightmap2 = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.WORLD_SURFACE_WG);
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int k = chunkPos.getMinBlockX();
        int l = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        int m = noiseSettings.getCellWidth();
        int n = noiseSettings.getCellHeight();
        int o = 16 / m;
        int p = 16 / m;

        for(int q = 0; q < o; ++q) {
            noiseChunk.advanceCellX(q);

            for(int r = 0; r < p; ++r) {
                ChunkSection levelChunkSection = chunk.getSection(chunk.getSectionsCount() - 1);

                for(int s = j - 1; s >= 0; --s) {
                    noiseChunk.selectCellYZ(s, r);

                    for(int t = n - 1; t >= 0; --t) {
                        int u = (i + s) * n + t;
                        int v = u & 15;
                        int w = chunk.getSectionIndex(u);
                        if (chunk.getSectionIndex(levelChunkSection.getYPosition()) != w) {
                            levelChunkSection = chunk.getSection(w);
                        }

                        double d = (double)t / (double)n;
                        noiseChunk.updateForY(d);

                        for(int x = 0; x < m; ++x) {
                            int y = k + q * m + x;
                            int z = y & 15;
                            double e = (double)x / (double)m;
                            noiseChunk.updateForX(e);

                            for(int aa = 0; aa < m; ++aa) {
                                int ab = l + r * m + aa;
                                int ac = ab & 15;
                                double f = (double)aa / (double)m;
                                noiseChunk.updateForZ(f);
                                IBlockData blockState = this.materialRule.apply(noiseChunk, y, u, ab);
                                if (blockState == null) {
                                    blockState = this.defaultBlock;
                                }

                                blockState = this.debugPreliminarySurfaceLevel(noiseChunk, y, u, ab, blockState);
                                if (blockState != AIR && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
                                    if (blockState.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutableBlockPos.set(y, u, ab);
                                        ((ProtoChunk)chunk).addLight(mutableBlockPos);
                                    }

                                    levelChunkSection.setType(z, v, ac, blockState, false);
                                    heightmap.update(z, u, ac, blockState);
                                    heightmap2.update(z, u, ac, blockState);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockState.getFluid().isEmpty()) {
                                        mutableBlockPos.set(y, u, ab);
                                        chunk.markPosForPostprocessing(mutableBlockPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        return chunk;
    }

    private IBlockData debugPreliminarySurfaceLevel(NoiseChunk chunkNoiseSampler, int x, int y, int z, IBlockData state) {
        return state;
    }

    @Override
    public int getGenerationDepth() {
        return this.settings.get().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.get().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.get().noiseSettings().minY();
    }

    @Override
    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getMobsFor(BiomeBase biome, StructureManager accessor, EnumCreatureType group, BlockPosition pos) {
        if (!accessor.hasAnyStructureAt(pos)) {
            return super.getMobsFor(biome, accessor, group, pos);
        } else {
            if (accessor.getStructureWithPieceAt(pos, StructureGenerator.SWAMP_HUT).isValid()) {
                if (group == EnumCreatureType.MONSTER) {
                    return WorldGenFeatureSwampHut.SWAMPHUT_ENEMIES;
                }

                if (group == EnumCreatureType.CREATURE) {
                    return WorldGenFeatureSwampHut.SWAMPHUT_ANIMALS;
                }
            }

            if (group == EnumCreatureType.MONSTER) {
                if (accessor.getStructureAt(pos, StructureGenerator.PILLAGER_OUTPOST).isValid()) {
                    return WorldGenFeaturePillagerOutpost.OUTPOST_ENEMIES;
                }

                if (accessor.getStructureAt(pos, StructureGenerator.OCEAN_MONUMENT).isValid()) {
                    return WorldGenMonument.MONUMENT_ENEMIES;
                }

                if (accessor.getStructureWithPieceAt(pos, StructureGenerator.NETHER_BRIDGE).isValid()) {
                    return WorldGenNether.FORTRESS_ENEMIES;
                }
            }

            return (group == EnumCreatureType.UNDERGROUND_WATER_CREATURE || group == EnumCreatureType.AXOLOTLS) && accessor.getStructureAt(pos, StructureGenerator.OCEAN_MONUMENT).isValid() ? BiomeSettingsMobs.EMPTY_MOB_LIST : super.getMobsFor(biome, accessor, group, pos);
        }
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess region) {
        if (!this.settings.get().disableMobGeneration()) {
            ChunkCoordIntPair chunkPos = region.getCenter();
            BiomeBase biome = region.getBiome(chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
            SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
            worldgenRandom.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(region, biome, chunkPos, worldgenRandom);
        }
    }

    /** @deprecated */
    @Deprecated
    public Optional<IBlockData> topMaterial(CarvingContext context, Function<BlockPosition, BiomeBase> posToBiome, IChunkAccess chunk, NoiseChunk chunkNoiseSampler, BlockPosition pos, boolean hasFluid) {
        return this.surfaceSystem.topMaterial(this.settings.get().surfaceRule(), context, posToBiome, chunk, chunkNoiseSampler, pos, hasFluid);
    }
}
