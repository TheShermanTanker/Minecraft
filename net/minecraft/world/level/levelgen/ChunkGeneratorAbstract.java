package net.minecraft.world.level.levelgen;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPosition;
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
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3Handler;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorOctaves;

public final class ChunkGeneratorAbstract extends ChunkGenerator {
    public static final Codec<ChunkGeneratorAbstract> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldChunkManager.CODEC.fieldOf("biome_source").forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.biomeSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.seed;
        }), GeneratorSettingBase.CODEC.fieldOf("settings").forGetter((noiseBasedChunkGenerator) -> {
            return noiseBasedChunkGenerator.settings;
        })).apply(instance, instance.stable(ChunkGeneratorAbstract::new));
    });
    private static final IBlockData AIR = Blocks.AIR.getBlockData();
    private static final IBlockData[] EMPTY_COLUMN = new IBlockData[0];
    private final int cellHeight;
    private final int cellWidth;
    final int cellCountX;
    final int cellCountY;
    final int cellCountZ;
    private final NoiseGenerator surfaceNoise;
    private final NoiseGeneratorNormal barrierNoise;
    private final NoiseGeneratorNormal waterLevelNoise;
    private final NoiseGeneratorNormal lavaNoise;
    protected final IBlockData defaultBlock;
    protected final IBlockData defaultFluid;
    private final long seed;
    protected final Supplier<GeneratorSettingBase> settings;
    private final int height;
    private final NoiseSampler sampler;
    private final BaseStoneSource baseStoneSource;
    final OreVeinifier oreVeinifier;
    final NoodleCavifier noodleCavifier;

    public ChunkGeneratorAbstract(WorldChunkManager biomeSource, long seed, Supplier<GeneratorSettingBase> settings) {
        this(biomeSource, biomeSource, seed, settings);
    }

    private ChunkGeneratorAbstract(WorldChunkManager populationSource, WorldChunkManager biomeSource, long seed, Supplier<GeneratorSettingBase> settings) {
        super(populationSource, biomeSource, settings.get().structureSettings(), seed);
        this.seed = seed;
        GeneratorSettingBase noiseGeneratorSettings = settings.get();
        this.settings = settings;
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        this.height = noiseSettings.height();
        this.cellHeight = QuartPos.toBlock(noiseSettings.noiseSizeVertical());
        this.cellWidth = QuartPos.toBlock(noiseSettings.noiseSizeHorizontal());
        this.defaultBlock = noiseGeneratorSettings.getDefaultBlock();
        this.defaultFluid = noiseGeneratorSettings.getDefaultFluid();
        this.cellCountX = 16 / this.cellWidth;
        this.cellCountY = noiseSettings.height() / this.cellHeight;
        this.cellCountZ = 16 / this.cellWidth;
        SeededRandom worldgenRandom = new SeededRandom(seed);
        BlendedNoise blendedNoise = new BlendedNoise(worldgenRandom);
        this.surfaceNoise = (NoiseGenerator)(noiseSettings.useSimplexSurfaceNoise() ? new NoiseGenerator3(worldgenRandom, IntStream.rangeClosed(-3, 0)) : new NoiseGeneratorOctaves(worldgenRandom, IntStream.rangeClosed(-3, 0)));
        worldgenRandom.consumeCount(2620);
        NoiseGeneratorOctaves perlinNoise = new NoiseGeneratorOctaves(worldgenRandom, IntStream.rangeClosed(-15, 0));
        NoiseGenerator3Handler simplexNoise;
        if (noiseSettings.islandNoiseOverride()) {
            SeededRandom worldgenRandom2 = new SeededRandom(seed);
            worldgenRandom2.consumeCount(17292);
            simplexNoise = new NoiseGenerator3Handler(worldgenRandom2);
        } else {
            simplexNoise = null;
        }

        this.barrierNoise = NoiseGeneratorNormal.create(new SimpleRandomSource(worldgenRandom.nextLong()), -3, 1.0D);
        this.waterLevelNoise = NoiseGeneratorNormal.create(new SimpleRandomSource(worldgenRandom.nextLong()), -3, 1.0D, 0.0D, 2.0D);
        this.lavaNoise = NoiseGeneratorNormal.create(new SimpleRandomSource(worldgenRandom.nextLong()), -1, 1.0D, 0.0D);
        NoiseModifier noiseModifier;
        if (noiseGeneratorSettings.isNoiseCavesEnabled()) {
            noiseModifier = new Cavifier(worldgenRandom, noiseSettings.minY() / this.cellHeight);
        } else {
            noiseModifier = NoiseModifier.PASSTHROUGH;
        }

        this.sampler = new NoiseSampler(populationSource, this.cellWidth, this.cellHeight, this.cellCountY, noiseSettings, blendedNoise, simplexNoise, perlinNoise, noiseModifier);
        this.baseStoneSource = new DepthBasedReplacingBaseStoneSource(seed, this.defaultBlock, Blocks.DEEPSLATE.getBlockData(), noiseGeneratorSettings);
        this.oreVeinifier = new OreVeinifier(seed, this.defaultBlock, this.cellWidth, this.cellHeight, noiseGeneratorSettings.noiseSettings().minY());
        this.noodleCavifier = new NoodleCavifier(seed);
    }

    private boolean isAquifersEnabled() {
        return this.settings.get().isAquifersEnabled();
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new ChunkGeneratorAbstract(this.biomeSource.withSeed(seed), seed, this.settings);
    }

    public boolean stable(long seed, ResourceKey<GeneratorSettingBase> settingsKey) {
        return this.seed == seed && this.settings.get().stable(settingsKey);
    }

    private double[] makeAndFillNoiseColumn(int x, int z, int minY, int noiseSizeY) {
        double[] ds = new double[noiseSizeY + 1];
        this.fillNoiseColumn(ds, x, z, minY, noiseSizeY);
        return ds;
    }

    private void fillNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        this.sampler.fillNoiseColumn(buffer, x, z, noiseSettings, this.getSeaLevel(), minY, noiseSizeY);
    }

    @Override
    public int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        int i = Math.max(this.settings.get().noiseSettings().minY(), world.getMinBuildHeight());
        int j = Math.min(this.settings.get().noiseSettings().minY() + this.settings.get().noiseSettings().height(), world.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, this.cellHeight);
        int l = MathHelper.intFloorDiv(j - i, this.cellHeight);
        return l <= 0 ? world.getMinBuildHeight() : this.iterateNoiseColumn(x, z, (IBlockData[])null, heightmap.isOpaque(), k, l).orElse(world.getMinBuildHeight());
    }

    @Override
    public BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world) {
        int i = Math.max(this.settings.get().noiseSettings().minY(), world.getMinBuildHeight());
        int j = Math.min(this.settings.get().noiseSettings().minY() + this.settings.get().noiseSettings().height(), world.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, this.cellHeight);
        int l = MathHelper.intFloorDiv(j - i, this.cellHeight);
        if (l <= 0) {
            return new BlockColumn(i, EMPTY_COLUMN);
        } else {
            IBlockData[] blockStates = new IBlockData[l * this.cellHeight];
            this.iterateNoiseColumn(x, z, blockStates, (Predicate<IBlockData>)null, k, l);
            return new BlockColumn(i, blockStates);
        }
    }

    @Override
    public BaseStoneSource getBaseStoneSource() {
        return this.baseStoneSource;
    }

    private OptionalInt iterateNoiseColumn(int x, int z, @Nullable IBlockData[] states, @Nullable Predicate<IBlockData> predicate, int minY, int noiseSizeY) {
        int i = SectionPosition.blockToSectionCoord(x);
        int j = SectionPosition.blockToSectionCoord(z);
        int k = Math.floorDiv(x, this.cellWidth);
        int l = Math.floorDiv(z, this.cellWidth);
        int m = Math.floorMod(x, this.cellWidth);
        int n = Math.floorMod(z, this.cellWidth);
        double d = (double)m / (double)this.cellWidth;
        double e = (double)n / (double)this.cellWidth;
        double[][] ds = new double[][]{this.makeAndFillNoiseColumn(k, l, minY, noiseSizeY), this.makeAndFillNoiseColumn(k, l + 1, minY, noiseSizeY), this.makeAndFillNoiseColumn(k + 1, l, minY, noiseSizeY), this.makeAndFillNoiseColumn(k + 1, l + 1, minY, noiseSizeY)};
        Aquifer aquifer = this.getAquifer(minY, noiseSizeY, new ChunkCoordIntPair(i, j));

        for(int o = noiseSizeY - 1; o >= 0; --o) {
            double f = ds[0][o];
            double g = ds[1][o];
            double h = ds[2][o];
            double p = ds[3][o];
            double q = ds[0][o + 1];
            double r = ds[1][o + 1];
            double s = ds[2][o + 1];
            double t = ds[3][o + 1];

            for(int u = this.cellHeight - 1; u >= 0; --u) {
                double v = (double)u / (double)this.cellHeight;
                double w = MathHelper.lerp3(v, d, e, f, q, h, s, g, r, p, t);
                int y = o * this.cellHeight + u;
                int aa = y + minY * this.cellHeight;
                IBlockData blockState = this.updateNoiseAndGenerateBaseState(Beardifier.NO_BEARDS, aquifer, this.baseStoneSource, NoiseModifier.PASSTHROUGH, x, aa, z, w);
                if (states != null) {
                    states[y] = blockState;
                }

                if (predicate != null && predicate.test(blockState)) {
                    return OptionalInt.of(aa + 1);
                }
            }
        }

        return OptionalInt.empty();
    }

    private Aquifer getAquifer(int startY, int deltaY, ChunkCoordIntPair pos) {
        return !this.isAquifersEnabled() ? Aquifer.createDisabled(this.getSeaLevel(), this.defaultFluid) : Aquifer.create(pos, this.barrierNoise, this.waterLevelNoise, this.lavaNoise, this.settings.get(), this.sampler, startY * this.cellHeight, deltaY * this.cellHeight);
    }

    protected IBlockData updateNoiseAndGenerateBaseState(Beardifier structures, Aquifer aquiferSampler, BaseStoneSource blockInterpolator, NoiseModifier noiseModifier, int i, int j, int k, double d) {
        double e = MathHelper.clamp(d / 200.0D, -1.0D, 1.0D);
        e = e / 2.0D - e * e * e / 24.0D;
        e = noiseModifier.modifyNoise(e, i, j, k);
        e = e + structures.beardifyOrBury(i, j, k);
        return aquiferSampler.computeState(blockInterpolator, i, j, k, e);
    }

    @Override
    public void buildBase(RegionLimitedWorldAccess region, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        SeededRandom worldgenRandom = new SeededRandom();
        worldgenRandom.setBaseChunkSeed(i, j);
        ChunkCoordIntPair chunkPos2 = chunk.getPos();
        int k = chunkPos2.getMinBlockX();
        int l = chunkPos2.getMinBlockZ();
        double d = 0.0625D;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int m = 0; m < 16; ++m) {
            for(int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, m, n) + 1;
                double e = this.surfaceNoise.getSurfaceNoiseValue((double)o * 0.0625D, (double)p * 0.0625D, 0.0625D, (double)m * 0.0625D) * 15.0D;
                int r = this.settings.get().getMinSurfaceLevel();
                region.getBiome(mutableBlockPos.set(k + m, q, l + n)).buildSurfaceAt(worldgenRandom, chunk, o, p, q, e, this.defaultBlock, this.defaultFluid, this.getSeaLevel(), r, region.getSeed());
            }
        }

        this.setBedrock(chunk, worldgenRandom);
    }

    private void setBedrock(IChunkAccess chunk, Random random) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int i = chunk.getPos().getMinBlockX();
        int j = chunk.getPos().getMinBlockZ();
        GeneratorSettingBase noiseGeneratorSettings = this.settings.get();
        int k = noiseGeneratorSettings.noiseSettings().minY();
        int l = k + noiseGeneratorSettings.getBedrockFloorPosition();
        int m = this.height - 1 + k - noiseGeneratorSettings.getBedrockRoofPosition();
        int n = 5;
        int o = chunk.getMinBuildHeight();
        int p = chunk.getMaxBuildHeight();
        boolean bl = m + 5 - 1 >= o && m < p;
        boolean bl2 = l + 5 - 1 >= o && l < p;
        if (bl || bl2) {
            for(BlockPosition blockPos : BlockPosition.betweenClosed(i, 0, j, i + 15, 0, j + 15)) {
                if (bl) {
                    for(int q = 0; q < 5; ++q) {
                        if (q <= random.nextInt(5)) {
                            chunk.setType(mutableBlockPos.set(blockPos.getX(), m - q, blockPos.getZ()), Blocks.BEDROCK.getBlockData(), false);
                        }
                    }
                }

                if (bl2) {
                    for(int r = 4; r >= 0; --r) {
                        if (r <= random.nextInt(5)) {
                            chunk.setType(mutableBlockPos.set(blockPos.getX(), l + r, blockPos.getZ()), Blocks.BEDROCK.getBlockData(), false);
                        }
                    }
                }
            }

        }
    }

    @Override
    public CompletableFuture<IChunkAccess> buildNoise(Executor executor, StructureManager accessor, IChunkAccess chunk) {
        NoiseSettings noiseSettings = this.settings.get().noiseSettings();
        int i = Math.max(noiseSettings.minY(), chunk.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), chunk.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, this.cellHeight);
        int l = MathHelper.intFloorDiv(j - i, this.cellHeight);
        if (l <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int m = chunk.getSectionIndex(l * this.cellHeight - 1 + i);
            int n = chunk.getSectionIndex(i);
            return CompletableFuture.supplyAsync(() -> {
                Set<ChunkSection> set = Sets.newHashSet();

                IChunkAccess var16;
                try {
                    for(int m = m; m >= n; --m) {
                        ChunkSection levelChunkSection = chunk.getOrCreateSection(m);
                        levelChunkSection.acquire();
                        set.add(levelChunkSection);
                    }

                    var16 = this.doFill(accessor, chunk, k, l);
                } finally {
                    for(ChunkSection levelChunkSection3 : set) {
                        levelChunkSection3.release();
                    }

                }

                return var16;
            }, SystemUtils.backgroundExecutor());
        }
    }

    private IChunkAccess doFill(StructureManager accessor, IChunkAccess chunk, int startY, int noiseSizeY) {
        HeightMap heightmap = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.OCEAN_FLOOR_WG);
        HeightMap heightmap2 = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.WORLD_SURFACE_WG);
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        Beardifier beardifier = new Beardifier(accessor, chunk);
        Aquifer aquifer = this.getAquifer(startY, noiseSizeY, chunkPos);
        NoiseInterpolator noiseInterpolator = new NoiseInterpolator(this.cellCountX, noiseSizeY, this.cellCountZ, chunkPos, startY, this::fillNoiseColumn);
        List<NoiseInterpolator> list = Lists.newArrayList(noiseInterpolator);
        Consumer<NoiseInterpolator> consumer = list::add;
        DoubleFunction<BaseStoneSource> doubleFunction = this.createBaseStoneSource(startY, chunkPos, consumer);
        DoubleFunction<NoiseModifier> doubleFunction2 = this.createCaveNoiseModifier(startY, chunkPos, consumer);
        list.forEach(NoiseInterpolator::initializeForFirstCellX);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int k = 0; k < this.cellCountX; ++k) {
            int l = k;
            list.forEach((noiseInterpolatorx) -> {
                noiseInterpolatorx.advanceCellX(l);
            });

            for(int m = 0; m < this.cellCountZ; ++m) {
                ChunkSection levelChunkSection = chunk.getOrCreateSection(chunk.getSectionsCount() - 1);

                for(int n = noiseSizeY - 1; n >= 0; --n) {
                    int o = m;
                    int p = n;
                    list.forEach((noiseInterpolatorx) -> {
                        noiseInterpolatorx.selectCellYZ(p, o);
                    });

                    for(int q = this.cellHeight - 1; q >= 0; --q) {
                        int r = (startY + n) * this.cellHeight + q;
                        int s = r & 15;
                        int t = chunk.getSectionIndex(r);
                        if (chunk.getSectionIndex(levelChunkSection.getYPosition()) != t) {
                            levelChunkSection = chunk.getOrCreateSection(t);
                        }

                        double d = (double)q / (double)this.cellHeight;
                        list.forEach((noiseInterpolatorx) -> {
                            noiseInterpolatorx.updateForY(d);
                        });

                        for(int u = 0; u < this.cellWidth; ++u) {
                            int v = i + k * this.cellWidth + u;
                            int w = v & 15;
                            double e = (double)u / (double)this.cellWidth;
                            list.forEach((noiseInterpolatorx) -> {
                                noiseInterpolatorx.updateForX(e);
                            });

                            for(int x = 0; x < this.cellWidth; ++x) {
                                int y = j + m * this.cellWidth + x;
                                int z = y & 15;
                                double f = (double)x / (double)this.cellWidth;
                                double g = noiseInterpolator.calculateValue(f);
                                IBlockData blockState = this.updateNoiseAndGenerateBaseState(beardifier, aquifer, doubleFunction.apply(f), doubleFunction2.apply(f), v, r, y, g);
                                if (blockState != AIR) {
                                    if (blockState.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutableBlockPos.set(v, r, y);
                                        ((ProtoChunk)chunk).addLight(mutableBlockPos);
                                    }

                                    levelChunkSection.setType(w, s, z, blockState, false);
                                    heightmap.update(w, r, z, blockState);
                                    heightmap2.update(w, r, z, blockState);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockState.getFluid().isEmpty()) {
                                        mutableBlockPos.set(v, r, y);
                                        chunk.getLiquidTicks().scheduleTick(mutableBlockPos, blockState.getFluid().getType(), 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            list.forEach(NoiseInterpolator::swapSlices);
        }

        return chunk;
    }

    private DoubleFunction<NoiseModifier> createCaveNoiseModifier(int minY, ChunkCoordIntPair pos, Consumer<NoiseInterpolator> consumer) {
        if (!this.settings.get().isNoodleCavesEnabled()) {
            return (d) -> {
                return NoiseModifier.PASSTHROUGH;
            };
        } else {
            ChunkGeneratorAbstract.NoodleCaveNoiseModifier noodleCaveNoiseModifier = new ChunkGeneratorAbstract.NoodleCaveNoiseModifier(pos, minY);
            noodleCaveNoiseModifier.listInterpolators(consumer);
            return noodleCaveNoiseModifier::prepare;
        }
    }

    private DoubleFunction<BaseStoneSource> createBaseStoneSource(int minY, ChunkCoordIntPair pos, Consumer<NoiseInterpolator> consumer) {
        if (!this.settings.get().isOreVeinsEnabled()) {
            return (d) -> {
                return this.baseStoneSource;
            };
        } else {
            ChunkGeneratorAbstract.OreVeinNoiseSource oreVeinNoiseSource = new ChunkGeneratorAbstract.OreVeinNoiseSource(pos, minY, this.seed + 1L);
            oreVeinNoiseSource.listInterpolators(consumer);
            BaseStoneSource baseStoneSource = (i, j, k) -> {
                IBlockData blockState = oreVeinNoiseSource.getBaseBlock(i, j, k);
                return blockState != this.defaultBlock ? blockState : this.baseStoneSource.getBaseBlock(i, j, k);
            };
            return (deltaZ) -> {
                oreVeinNoiseSource.prepare(deltaZ);
                return baseStoneSource;
            };
        }
    }

    @Override
    protected Aquifer createAquifer(IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = Math.max(this.settings.get().noiseSettings().minY(), chunk.getMinBuildHeight());
        int j = MathHelper.intFloorDiv(i, this.cellHeight);
        return this.getAquifer(j, this.cellCountY, chunkPos);
    }

    @Override
    public int getGenerationDepth() {
        return this.height;
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
        if (accessor.getStructureAt(pos, true, StructureGenerator.SWAMP_HUT).isValid()) {
            if (group == EnumCreatureType.MONSTER) {
                return StructureGenerator.SWAMP_HUT.getSpecialEnemies();
            }

            if (group == EnumCreatureType.CREATURE) {
                return StructureGenerator.SWAMP_HUT.getSpecialAnimals();
            }
        }

        if (group == EnumCreatureType.MONSTER) {
            if (accessor.getStructureAt(pos, false, StructureGenerator.PILLAGER_OUTPOST).isValid()) {
                return StructureGenerator.PILLAGER_OUTPOST.getSpecialEnemies();
            }

            if (accessor.getStructureAt(pos, false, StructureGenerator.OCEAN_MONUMENT).isValid()) {
                return StructureGenerator.OCEAN_MONUMENT.getSpecialEnemies();
            }

            if (accessor.getStructureAt(pos, true, StructureGenerator.NETHER_BRIDGE).isValid()) {
                return StructureGenerator.NETHER_BRIDGE.getSpecialEnemies();
            }
        }

        return group == EnumCreatureType.UNDERGROUND_WATER_CREATURE && accessor.getStructureAt(pos, false, StructureGenerator.OCEAN_MONUMENT).isValid() ? StructureGenerator.OCEAN_MONUMENT.getSpecialUndergroundWaterAnimals() : super.getMobsFor(biome, accessor, group, pos);
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess region) {
        if (!this.settings.get().disableMobGeneration()) {
            ChunkCoordIntPair chunkPos = region.getCenter();
            BiomeBase biome = region.getBiome(chunkPos.getWorldPosition());
            SeededRandom worldgenRandom = new SeededRandom();
            worldgenRandom.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(region, biome, chunkPos, worldgenRandom);
        }
    }

    class NoodleCaveNoiseModifier implements NoiseModifier {
        private final NoiseInterpolator toggle;
        private final NoiseInterpolator thickness;
        private final NoiseInterpolator ridgeA;
        private final NoiseInterpolator ridgeB;
        private double factorZ;

        public NoodleCaveNoiseModifier(ChunkCoordIntPair pos, int minY) {
            this.toggle = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.noodleCavifier::fillToggleNoiseColumn);
            this.thickness = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.noodleCavifier::fillThicknessNoiseColumn);
            this.ridgeA = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.noodleCavifier::fillRidgeANoiseColumn);
            this.ridgeB = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.noodleCavifier::fillRidgeBNoiseColumn);
        }

        public NoiseModifier prepare(double deltaZ) {
            this.factorZ = deltaZ;
            return this;
        }

        @Override
        public double modifyNoise(double weight, int x, int y, int z) {
            double d = this.toggle.calculateValue(this.factorZ);
            double e = this.thickness.calculateValue(this.factorZ);
            double f = this.ridgeA.calculateValue(this.factorZ);
            double g = this.ridgeB.calculateValue(this.factorZ);
            return ChunkGeneratorAbstract.this.noodleCavifier.noodleCavify(weight, x, y, z, d, e, f, g, ChunkGeneratorAbstract.this.getMinY());
        }

        public void listInterpolators(Consumer<NoiseInterpolator> f) {
            f.accept(this.toggle);
            f.accept(this.thickness);
            f.accept(this.ridgeA);
            f.accept(this.ridgeB);
        }
    }

    class OreVeinNoiseSource implements BaseStoneSource {
        private final NoiseInterpolator veininess;
        private final NoiseInterpolator veinA;
        private final NoiseInterpolator veinB;
        private double factorZ;
        private final long seed;
        private final SeededRandom random = new SeededRandom();

        public OreVeinNoiseSource(ChunkCoordIntPair pos, int minY, long seed) {
            this.veininess = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.oreVeinifier::fillVeininessNoiseColumn);
            this.veinA = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.oreVeinifier::fillNoiseColumnA);
            this.veinB = new NoiseInterpolator(ChunkGeneratorAbstract.this.cellCountX, ChunkGeneratorAbstract.this.cellCountY, ChunkGeneratorAbstract.this.cellCountZ, pos, minY, ChunkGeneratorAbstract.this.oreVeinifier::fillNoiseColumnB);
            this.seed = seed;
        }

        public void listInterpolators(Consumer<NoiseInterpolator> f) {
            f.accept(this.veininess);
            f.accept(this.veinA);
            f.accept(this.veinB);
        }

        public void prepare(double deltaZ) {
            this.factorZ = deltaZ;
        }

        @Override
        public IBlockData getBaseBlock(int x, int y, int z) {
            double d = this.veininess.calculateValue(this.factorZ);
            double e = this.veinA.calculateValue(this.factorZ);
            double f = this.veinB.calculateValue(this.factorZ);
            this.random.setBaseStoneSeed(this.seed, x, y, z);
            return ChunkGeneratorAbstract.this.oreVeinifier.oreVeinify(this.random, x, y, z, d, e, f);
        }
    }
}
