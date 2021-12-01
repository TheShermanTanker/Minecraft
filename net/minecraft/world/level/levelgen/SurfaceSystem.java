package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;
import net.minecraft.world.level.material.Material;

public class SurfaceSystem {
    private static final IBlockData WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getBlockData();
    private static final IBlockData ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getBlockData();
    private static final IBlockData TERRACOTTA = Blocks.TERRACOTTA.getBlockData();
    private static final IBlockData YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.getBlockData();
    private static final IBlockData BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.getBlockData();
    private static final IBlockData RED_TERRACOTTA = Blocks.RED_TERRACOTTA.getBlockData();
    private static final IBlockData LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.getBlockData();
    private static final IBlockData PACKED_ICE = Blocks.PACKED_ICE.getBlockData();
    private static final IBlockData SNOW_BLOCK = Blocks.SNOW_BLOCK.getBlockData();
    private final IBlockData defaultBlock;
    private final int seaLevel;
    private final IBlockData[] clayBands;
    private final NoiseGeneratorNormal clayBandsOffsetNoise;
    private final NoiseGeneratorNormal badlandsPillarNoise;
    private final NoiseGeneratorNormal badlandsPillarRoofNoise;
    private final NoiseGeneratorNormal badlandsSurfaceNoise;
    private final NoiseGeneratorNormal icebergPillarNoise;
    private final NoiseGeneratorNormal icebergPillarRoofNoise;
    private final NoiseGeneratorNormal icebergSurfaceNoise;
    private final IRegistry<NormalNoise$NoiseParameters> noises;
    private final Map<ResourceKey<NormalNoise$NoiseParameters>, NoiseGeneratorNormal> noiseIntances = new ConcurrentHashMap<>();
    private final Map<MinecraftKey, PositionalRandomFactory> positionalRandoms = new ConcurrentHashMap<>();
    private final PositionalRandomFactory randomFactory;
    private final NoiseGeneratorNormal surfaceNoise;
    private final NoiseGeneratorNormal surfaceSecondaryNoise;

    public SurfaceSystem(IRegistry<NormalNoise$NoiseParameters> noiseRegistry, IBlockData defaultState, int seaLevel, long seed, WorldgenRandom$Algorithm randomProvider) {
        this.noises = noiseRegistry;
        this.defaultBlock = defaultState;
        this.seaLevel = seaLevel;
        this.randomFactory = randomProvider.newInstance(seed).forkPositional();
        this.clayBandsOffsetNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.CLAY_BANDS_OFFSET);
        this.clayBands = generateBands(this.randomFactory.fromHashOf(new MinecraftKey("clay_bands")));
        this.surfaceNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.SURFACE);
        this.surfaceSecondaryNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.SURFACE_SECONDARY);
        this.badlandsPillarNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.BADLANDS_PILLAR);
        this.badlandsPillarRoofNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.BADLANDS_PILLAR_ROOF);
        this.badlandsSurfaceNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.BADLANDS_SURFACE);
        this.icebergPillarNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.ICEBERG_PILLAR);
        this.icebergPillarRoofNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.ICEBERG_PILLAR_ROOF);
        this.icebergSurfaceNoise = Noises.instantiate(noiseRegistry, this.randomFactory, Noises.ICEBERG_SURFACE);
    }

    protected NoiseGeneratorNormal getOrCreateNoise(ResourceKey<NormalNoise$NoiseParameters> noise) {
        return this.noiseIntances.computeIfAbsent(noise, (resourceKey2) -> {
            return Noises.instantiate(this.noises, this.randomFactory, noise);
        });
    }

    public PositionalRandomFactory getOrCreateRandomFactory(MinecraftKey id) {
        return this.positionalRandoms.computeIfAbsent(id, (i) -> {
            return this.randomFactory.fromHashOf(id).forkPositional();
        });
    }

    public void buildSurface(BiomeManager biomeAccess, IRegistry<BiomeBase> biomeRegistry, boolean useLegacyRandom, WorldGenerationContext context, IChunkAccess chunk, NoiseChunk chunkNoiseSampler, SurfaceRules.RuleSource surfaceRule) {
        final BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        final ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        BlockColumn blockColumn = new BlockColumn() {
            @Override
            public IBlockData getBlock(int y) {
                return chunk.getType(mutableBlockPos.setY(y));
            }

            @Override
            public void setBlock(int y, IBlockData state) {
                IWorldHeightAccess levelHeightAccessor = chunk.getHeightAccessorForGeneration();
                if (y >= levelHeightAccessor.getMinBuildHeight() && y < levelHeightAccessor.getMaxBuildHeight()) {
                    chunk.setType(mutableBlockPos.setY(y), state, false);
                    if (!state.getFluid().isEmpty()) {
                        chunk.markPosForPostprocessing(mutableBlockPos);
                    }
                }

            }

            @Override
            public String toString() {
                return "ChunkBlockColumn " + chunkPos;
            }
        };
        SurfaceRules.Context context2 = new SurfaceRules.Context(this, chunk, chunkNoiseSampler, biomeAccess::getBiome, biomeRegistry, context);
        SurfaceRules.SurfaceRule surfaceRule2 = surfaceRule.apply(context2);
        BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

        for(int k = 0; k < 16; ++k) {
            for(int l = 0; l < 16; ++l) {
                int m = i + k;
                int n = j + l;
                int o = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, k, l) + 1;
                mutableBlockPos.setX(m).setZ(n);
                BiomeBase biome = biomeAccess.getBiome(mutableBlockPos2.set(m, useLegacyRandom ? 0 : o, n));
                ResourceKey<BiomeBase> resourceKey = biomeRegistry.getResourceKey(biome).orElseThrow(() -> {
                    return new IllegalStateException("Unregistered biome: " + biome);
                });
                if (resourceKey == Biomes.ERODED_BADLANDS) {
                    this.erodedBadlandsExtension(blockColumn, m, n, o, chunk);
                }

                int p = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, k, l) + 1;
                context2.updateXZ(m, n);
                int q = 0;
                int r = Integer.MIN_VALUE;
                int s = Integer.MAX_VALUE;
                int t = chunk.getMinBuildHeight();

                for(int u = p; u >= t; --u) {
                    IBlockData blockState = blockColumn.getBlock(u);
                    if (blockState.isAir()) {
                        q = 0;
                        r = Integer.MIN_VALUE;
                    } else if (!blockState.getFluid().isEmpty()) {
                        if (r == Integer.MIN_VALUE) {
                            r = u + 1;
                        }
                    } else {
                        if (s >= u) {
                            s = DimensionManager.WAY_BELOW_MIN_Y;

                            for(int v = u - 1; v >= t - 1; --v) {
                                IBlockData blockState2 = blockColumn.getBlock(v);
                                if (!this.isStone(blockState2)) {
                                    s = v + 1;
                                    break;
                                }
                            }
                        }

                        ++q;
                        int w = u - s + 1;
                        context2.updateY(q, w, r, m, u, n);
                        if (blockState == this.defaultBlock) {
                            IBlockData blockState3 = surfaceRule2.tryApply(m, u, n);
                            if (blockState3 != null) {
                                blockColumn.setBlock(u, blockState3);
                            }
                        }
                    }
                }

                if (resourceKey == Biomes.FROZEN_OCEAN || resourceKey == Biomes.DEEP_FROZEN_OCEAN) {
                    this.frozenOceanExtension(context2.getMinSurfaceLevel(), biome, blockColumn, mutableBlockPos2, m, n, o);
                }
            }
        }

    }

    protected int getSurfaceDepth(int i, int j) {
        return this.getSurfaceDepth(this.surfaceNoise, i, j);
    }

    protected int getSurfaceSecondaryDepth(int i, int j) {
        return this.getSurfaceDepth(this.surfaceSecondaryNoise, i, j);
    }

    private int getSurfaceDepth(NoiseGeneratorNormal normalNoise, int i, int j) {
        return (int)(normalNoise.getValue((double)i, 0.0D, (double)j) * 2.75D + 3.0D + this.randomFactory.at(i, 0, j).nextDouble() * 0.25D);
    }

    private boolean isStone(IBlockData state) {
        return !state.isAir() && state.getFluid().isEmpty();
    }

    /** @deprecated */
    @Deprecated
    public Optional<IBlockData> topMaterial(SurfaceRules.RuleSource rule, CarvingContext context, Function<BlockPosition, BiomeBase> posToBiome, IChunkAccess chunk, NoiseChunk chunkNoiseSampler, BlockPosition pos, boolean hasFluid) {
        SurfaceRules.Context context2 = new SurfaceRules.Context(this, chunk, chunkNoiseSampler, posToBiome, context.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), context);
        SurfaceRules.SurfaceRule surfaceRule = rule.apply(context2);
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        context2.updateXZ(i, k);
        context2.updateY(1, 1, hasFluid ? j + 1 : Integer.MIN_VALUE, i, j, k);
        IBlockData blockState = surfaceRule.tryApply(i, j, k);
        return Optional.ofNullable(blockState);
    }

    private void erodedBadlandsExtension(BlockColumn column, int x, int z, int surfaceY, IWorldHeightAccess chunk) {
        double d = 0.2D;
        double e = Math.min(Math.abs(this.badlandsSurfaceNoise.getValue((double)x, 0.0D, (double)z) * 8.25D), this.badlandsPillarNoise.getValue((double)x * 0.2D, 0.0D, (double)z * 0.2D) * 15.0D);
        if (!(e <= 0.0D)) {
            double f = 0.75D;
            double g = 1.5D;
            double h = Math.abs(this.badlandsPillarRoofNoise.getValue((double)x * 0.75D, 0.0D, (double)z * 0.75D) * 1.5D);
            double i = 64.0D + Math.min(e * e * 2.5D, Math.ceil(h * 50.0D) + 24.0D);
            int j = MathHelper.floor(i);
            if (surfaceY <= j) {
                for(int k = j; k >= chunk.getMinBuildHeight(); --k) {
                    IBlockData blockState = column.getBlock(k);
                    if (blockState.is(this.defaultBlock.getBlock())) {
                        break;
                    }

                    if (blockState.is(Blocks.WATER)) {
                        return;
                    }
                }

                for(int l = j; l >= chunk.getMinBuildHeight() && column.getBlock(l).isAir(); --l) {
                    column.setBlock(l, this.defaultBlock);
                }

            }
        }
    }

    private void frozenOceanExtension(int minY, BiomeBase biome, BlockColumn column, BlockPosition.MutableBlockPosition mutablePos, int x, int z, int surfaceY) {
        double d = 1.28D;
        double e = Math.min(Math.abs(this.icebergSurfaceNoise.getValue((double)x, 0.0D, (double)z) * 8.25D), this.icebergPillarNoise.getValue((double)x * 1.28D, 0.0D, (double)z * 1.28D) * 15.0D);
        if (!(e <= 1.8D)) {
            double f = 1.17D;
            double g = 1.5D;
            double h = Math.abs(this.icebergPillarRoofNoise.getValue((double)x * 1.17D, 0.0D, (double)z * 1.17D) * 1.5D);
            double i = Math.min(e * e * 1.2D, Math.ceil(h * 40.0D) + 14.0D);
            if (biome.shouldMeltFrozenOceanIcebergSlightly(mutablePos.set(x, 63, z))) {
                i -= 2.0D;
            }

            double j;
            if (i > 2.0D) {
                j = (double)this.seaLevel - i - 7.0D;
                i = i + (double)this.seaLevel;
            } else {
                i = 0.0D;
                j = 0.0D;
            }

            double l = i;
            RandomSource randomSource = this.randomFactory.at(x, 0, z);
            int m = 2 + randomSource.nextInt(4);
            int n = this.seaLevel + 18 + randomSource.nextInt(10);
            int o = 0;

            for(int p = Math.max(surfaceY, (int)i + 1); p >= minY; --p) {
                if (column.getBlock(p).isAir() && p < (int)l && randomSource.nextDouble() > 0.01D || column.getBlock(p).getMaterial() == Material.WATER && p > (int)j && p < this.seaLevel && j != 0.0D && randomSource.nextDouble() > 0.15D) {
                    if (o <= m && p > n) {
                        column.setBlock(p, SNOW_BLOCK);
                        ++o;
                    } else {
                        column.setBlock(p, PACKED_ICE);
                    }
                }
            }

        }
    }

    private static IBlockData[] generateBands(RandomSource random) {
        IBlockData[] blockStates = new IBlockData[192];
        Arrays.fill(blockStates, TERRACOTTA);

        for(int i = 0; i < blockStates.length; ++i) {
            i += random.nextInt(5) + 1;
            if (i < blockStates.length) {
                blockStates[i] = ORANGE_TERRACOTTA;
            }
        }

        makeBands(random, blockStates, 1, YELLOW_TERRACOTTA);
        makeBands(random, blockStates, 2, BROWN_TERRACOTTA);
        makeBands(random, blockStates, 1, RED_TERRACOTTA);
        int j = random.nextIntBetweenInclusive(9, 15);
        int k = 0;

        for(int l = 0; k < j && l < blockStates.length; l += random.nextInt(16) + 4) {
            blockStates[l] = WHITE_TERRACOTTA;
            if (l - 1 > 0 && random.nextBoolean()) {
                blockStates[l - 1] = LIGHT_GRAY_TERRACOTTA;
            }

            if (l + 1 < blockStates.length && random.nextBoolean()) {
                blockStates[l + 1] = LIGHT_GRAY_TERRACOTTA;
            }

            ++k;
        }

        return blockStates;
    }

    private static void makeBands(RandomSource random, IBlockData[] terracottaBands, int minBandSize, IBlockData state) {
        int i = random.nextIntBetweenInclusive(6, 15);

        for(int j = 0; j < i; ++j) {
            int k = minBandSize + random.nextInt(3);
            int l = random.nextInt(terracottaBands.length);

            for(int m = 0; l + m < terracottaBands.length && m < k; ++m) {
                terracottaBands[l + m] = state;
            }
        }

    }

    protected IBlockData getBand(int x, int y, int z) {
        int i = (int)Math.round(this.clayBandsOffsetNoise.getValue((double)x, 0.0D, (double)z) * 4.0D);
        return this.clayBands[(y + i + this.clayBands.length) % this.clayBands.length];
    }
}
