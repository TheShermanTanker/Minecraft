package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.EnumDirection8;
import net.minecraft.core.QuartPos;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.TerrainInfo;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;

public class Blender {
    private static final Blender EMPTY = new Blender((RegionLimitedWorldAccess)null, List.of(), List.of()) {
        @Override
        public TerrainInfo blendOffsetAndFactor(int i, int j, TerrainInfo terrainInfo) {
            return terrainInfo;
        }

        @Override
        public double blendDensity(int i, int j, int k, double d) {
            return d;
        }

        @Override
        public BiomeResolver getBiomeResolver(BiomeResolver biomeSupplier) {
            return biomeSupplier;
        }
    };
    private static final NoiseGeneratorNormal SHIFT_NOISE = NoiseGeneratorNormal.create(new XoroshiroRandomSource(42L), RegistryGeneration.NOISE.getOrThrow(Noises.SHIFT));
    private static final int HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
    private static final int HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
    private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
    private static final int DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
    private static final double BLENDING_FACTOR = 10.0D;
    private static final double BLENDING_JAGGEDNESS = 0.0D;
    private static final double OLD_CHUNK_Y_RADIUS = (double)BlendingData.AREA_WITH_OLD_GENERATION.getHeight() / 2.0D;
    private static final double OLD_CHUNK_CENTER_Y = (double)BlendingData.AREA_WITH_OLD_GENERATION.getMinBuildHeight() + OLD_CHUNK_Y_RADIUS;
    private static final double OLD_CHUNK_XZ_RADIUS = 8.0D;
    private final RegionLimitedWorldAccess region;
    private final List<Blender.PositionedBlendingData> heightData;
    private final List<Blender.PositionedBlendingData> densityData;

    public static Blender empty() {
        return EMPTY;
    }

    public static Blender of(@Nullable RegionLimitedWorldAccess chunkRegion) {
        if (chunkRegion == null) {
            return EMPTY;
        } else {
            List<Blender.PositionedBlendingData> list = Lists.newArrayList();
            List<Blender.PositionedBlendingData> list2 = Lists.newArrayList();
            ChunkCoordIntPair chunkPos = chunkRegion.getCenter();

            for(int i = -HEIGHT_BLENDING_RANGE_CHUNKS; i <= HEIGHT_BLENDING_RANGE_CHUNKS; ++i) {
                for(int j = -HEIGHT_BLENDING_RANGE_CHUNKS; j <= HEIGHT_BLENDING_RANGE_CHUNKS; ++j) {
                    int k = chunkPos.x + i;
                    int l = chunkPos.z + j;
                    BlendingData blendingData = BlendingData.getOrUpdateBlendingData(chunkRegion, k, l);
                    if (blendingData != null) {
                        Blender.PositionedBlendingData positionedBlendingData = new Blender.PositionedBlendingData(k, l, blendingData);
                        list.add(positionedBlendingData);
                        if (i >= -DENSITY_BLENDING_RANGE_CHUNKS && i <= DENSITY_BLENDING_RANGE_CHUNKS && j >= -DENSITY_BLENDING_RANGE_CHUNKS && j <= DENSITY_BLENDING_RANGE_CHUNKS) {
                            list2.add(positionedBlendingData);
                        }
                    }
                }
            }

            return list.isEmpty() && list2.isEmpty() ? EMPTY : new Blender(chunkRegion, list, list2);
        }
    }

    Blender(RegionLimitedWorldAccess chunkRegion, List<Blender.PositionedBlendingData> list, List<Blender.PositionedBlendingData> list2) {
        this.region = chunkRegion;
        this.heightData = list;
        this.densityData = list2;
    }

    public TerrainInfo blendOffsetAndFactor(int i, int j, TerrainInfo terrainInfo) {
        int k = QuartPos.fromBlock(i);
        int l = QuartPos.fromBlock(j);
        double d = this.getBlendingDataValue(k, 0, l, BlendingData::getHeight);
        if (d != Double.MAX_VALUE) {
            return new TerrainInfo(heightToOffset(d), 10.0D, 0.0D);
        } else {
            MutableDouble mutableDouble = new MutableDouble(0.0D);
            MutableDouble mutableDouble2 = new MutableDouble(0.0D);
            MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);

            for(Blender.PositionedBlendingData positionedBlendingData : this.heightData) {
                positionedBlendingData.blendingData.iterateHeights(QuartPos.fromSection(positionedBlendingData.chunkX), QuartPos.fromSection(positionedBlendingData.chunkZ), (kx, lx, dx) -> {
                    double e = MathHelper.length((double)(k - kx), (double)(l - lx));
                    if (!(e > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
                        if (e < mutableDouble3.doubleValue()) {
                            mutableDouble3.setValue(e);
                        }

                        double f = 1.0D / (e * e * e * e);
                        mutableDouble2.add(dx * f);
                        mutableDouble.add(f);
                    }
                });
            }

            if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
                return terrainInfo;
            } else {
                double e = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
                double f = MathHelper.clamp(mutableDouble3.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
                f = 3.0D * f * f - 2.0D * f * f * f;
                double g = MathHelper.lerp(f, heightToOffset(e), terrainInfo.offset());
                double h = MathHelper.lerp(f, 10.0D, terrainInfo.factor());
                double m = MathHelper.lerp(f, 0.0D, terrainInfo.jaggedness());
                return new TerrainInfo(g, h, m);
            }
        }
    }

    private static double heightToOffset(double d) {
        double e = 1.0D;
        double f = d + 0.5D;
        double g = MathHelper.positiveModulo(f, 8.0D);
        return 1.0D * (32.0D * (f - 128.0D) - 3.0D * (f - 120.0D) * g + 3.0D * g * g) / (128.0D * (32.0D - 3.0D * g));
    }

    public double blendDensity(int i, int j, int k, double d) {
        int l = QuartPos.fromBlock(i);
        int m = j / 8;
        int n = QuartPos.fromBlock(k);
        double e = this.getBlendingDataValue(l, m, n, BlendingData::getDensity);
        if (e != Double.MAX_VALUE) {
            return e;
        } else {
            MutableDouble mutableDouble = new MutableDouble(0.0D);
            MutableDouble mutableDouble2 = new MutableDouble(0.0D);
            MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);

            for(Blender.PositionedBlendingData positionedBlendingData : this.densityData) {
                positionedBlendingData.blendingData.iterateDensities(QuartPos.fromSection(positionedBlendingData.chunkX), QuartPos.fromSection(positionedBlendingData.chunkZ), m - 1, m + 1, (lx, mx, nx, dx) -> {
                    double e = MathHelper.length((double)(l - lx), (double)((m - mx) * 2), (double)(n - nx));
                    if (!(e > 2.0D)) {
                        if (e < mutableDouble3.doubleValue()) {
                            mutableDouble3.setValue(e);
                        }

                        double f = 1.0D / (e * e * e * e);
                        mutableDouble2.add(dx * f);
                        mutableDouble.add(f);
                    }
                });
            }

            if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
                return d;
            } else {
                double f = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
                double g = MathHelper.clamp(mutableDouble3.doubleValue() / 3.0D, 0.0D, 1.0D);
                return MathHelper.lerp(g, f, d);
            }
        }
    }

    private double getBlendingDataValue(int i, int j, int k, Blender.CellValueGetter cellValueGetter) {
        int l = QuartPos.toSection(i);
        int m = QuartPos.toSection(k);
        boolean bl = (i & 3) == 0;
        boolean bl2 = (k & 3) == 0;
        double d = this.getBlendingDataValue(cellValueGetter, l, m, i, j, k);
        if (d == Double.MAX_VALUE) {
            if (bl && bl2) {
                d = this.getBlendingDataValue(cellValueGetter, l - 1, m - 1, i, j, k);
            }

            if (d == Double.MAX_VALUE) {
                if (bl) {
                    d = this.getBlendingDataValue(cellValueGetter, l - 1, m, i, j, k);
                }

                if (d == Double.MAX_VALUE && bl2) {
                    d = this.getBlendingDataValue(cellValueGetter, l, m - 1, i, j, k);
                }
            }
        }

        return d;
    }

    private double getBlendingDataValue(Blender.CellValueGetter cellValueGetter, int i, int j, int k, int l, int m) {
        BlendingData blendingData = BlendingData.getOrUpdateBlendingData(this.region, i, j);
        return blendingData != null ? cellValueGetter.get(blendingData, k - QuartPos.fromSection(i), l, m - QuartPos.fromSection(j)) : Double.MAX_VALUE;
    }

    public BiomeResolver getBiomeResolver(BiomeResolver biomeSupplier) {
        return (x, y, z, noise) -> {
            BiomeBase biome = this.blendBiome(x, y, z);
            return biome == null ? biomeSupplier.getNoiseBiome(x, y, z, noise) : biome;
        };
    }

    @Nullable
    private BiomeBase blendBiome(int x, int y, int z) {
        double d = (double)x + SHIFT_NOISE.getValue((double)x, 0.0D, (double)z) * 12.0D;
        double e = (double)z + SHIFT_NOISE.getValue((double)z, (double)x, 0.0D) * 12.0D;
        MutableDouble mutableDouble = new MutableDouble(Double.POSITIVE_INFINITY);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        MutableObject<ChunkCoordIntPair> mutableObject = new MutableObject<>();

        for(Blender.PositionedBlendingData positionedBlendingData : this.heightData) {
            positionedBlendingData.blendingData.iterateHeights(QuartPos.fromSection(positionedBlendingData.chunkX), QuartPos.fromSection(positionedBlendingData.chunkZ), (i, j, f) -> {
                double g = MathHelper.length(d - (double)i, e - (double)j);
                if (!(g > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
                    if (g < mutableDouble.doubleValue()) {
                        mutableObject.setValue(new ChunkCoordIntPair(positionedBlendingData.chunkX, positionedBlendingData.chunkZ));
                        mutableBlockPos.set(i, QuartPos.fromBlock(MathHelper.floor(f)), j);
                        mutableDouble.setValue(g);
                    }

                }
            });
        }

        if (mutableDouble.doubleValue() == Double.POSITIVE_INFINITY) {
            return null;
        } else {
            double f = MathHelper.clamp(mutableDouble.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
            if (f > 0.5D) {
                return null;
            } else {
                IChunkAccess chunkAccess = this.region.getChunkAt((mutableObject.getValue()).x, (mutableObject.getValue()).z);
                return chunkAccess.getBiome(Math.min(mutableBlockPos.getX() & 3, 3), mutableBlockPos.getY(), Math.min(mutableBlockPos.getZ() & 3, 3));
            }
        }
    }

    public static void generateBorderTicks(RegionLimitedWorldAccess chunkRegion, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        boolean bl = chunk.isOldNoiseGeneration();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        BlockPosition blockPos = new BlockPosition(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        int i = BlendingData.AREA_WITH_OLD_GENERATION.getMinBuildHeight();
        int j = BlendingData.AREA_WITH_OLD_GENERATION.getMaxBuildHeight() - 1;
        if (bl) {
            for(int k = 0; k < 16; ++k) {
                for(int l = 0; l < 16; ++l) {
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, i - 1, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, i, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, j, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, j + 1, l));
                }
            }
        }

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            if (chunkRegion.getChunkAt(chunkPos.x + direction.getAdjacentX(), chunkPos.z + direction.getAdjacentZ()).isOldNoiseGeneration() != bl) {
                int m = direction == EnumDirection.EAST ? 15 : 0;
                int n = direction == EnumDirection.WEST ? 0 : 15;
                int o = direction == EnumDirection.SOUTH ? 15 : 0;
                int p = direction == EnumDirection.NORTH ? 0 : 15;

                for(int q = m; q <= n; ++q) {
                    for(int r = o; r <= p; ++r) {
                        int s = Math.min(j, chunk.getHighestBlock(HeightMap.Type.MOTION_BLOCKING, q, r)) + 1;

                        for(int t = i; t < s; ++t) {
                            generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, q, t, r));
                        }
                    }
                }
            }
        }

    }

    private static void generateBorderTick(IChunkAccess chunk, BlockPosition pos) {
        IBlockData blockState = chunk.getType(pos);
        if (blockState.is(TagsBlock.LEAVES)) {
            chunk.markPosForPostprocessing(pos);
        }

        Fluid fluidState = chunk.getFluid(pos);
        if (!fluidState.isEmpty()) {
            chunk.markPosForPostprocessing(pos);
        }

    }

    public static void addAroundOldChunksCarvingMaskFilter(GeneratorAccessSeed worldGenLevel, ProtoChunk protoChunk) {
        ChunkCoordIntPair chunkPos = protoChunk.getPos();
        Blender.DistanceGetter distanceGetter = makeOldChunkDistanceGetter(protoChunk.isOldNoiseGeneration(), BlendingData.sideByGenerationAge(worldGenLevel, chunkPos.x, chunkPos.z, true));
        if (distanceGetter != null) {
            CarvingMask.Mask mask = (i, j, k) -> {
                double d = (double)i + 0.5D + SHIFT_NOISE.getValue((double)i, (double)j, (double)k) * 4.0D;
                double e = (double)j + 0.5D + SHIFT_NOISE.getValue((double)j, (double)k, (double)i) * 4.0D;
                double f = (double)k + 0.5D + SHIFT_NOISE.getValue((double)k, (double)i, (double)j) * 4.0D;
                return distanceGetter.getDistance(d, e, f) < 4.0D;
            };
            Stream.of(WorldGenStage.Features.values()).map(protoChunk::getOrCreateCarvingMask).forEach((carvingMask) -> {
                carvingMask.setAdditionalMask(mask);
            });
        }
    }

    @Nullable
    public static Blender.DistanceGetter makeOldChunkDistanceGetter(boolean bl, Set<EnumDirection8> set) {
        if (!bl && set.isEmpty()) {
            return null;
        } else {
            List<Blender.DistanceGetter> list = Lists.newArrayList();
            if (bl) {
                list.add(makeOffsetOldChunkDistanceGetter((EnumDirection8)null));
            }

            set.forEach((direction8) -> {
                list.add(makeOffsetOldChunkDistanceGetter(direction8));
            });
            return (d, e, f) -> {
                double g = Double.POSITIVE_INFINITY;

                for(Blender.DistanceGetter distanceGetter : list) {
                    double h = distanceGetter.getDistance(d, e, f);
                    if (h < g) {
                        g = h;
                    }
                }

                return g;
            };
        }
    }

    private static Blender.DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable EnumDirection8 direction8) {
        double d = 0.0D;
        double e = 0.0D;
        if (direction8 != null) {
            for(EnumDirection direction : direction8.getDirections()) {
                d += (double)(direction.getAdjacentX() * 16);
                e += (double)(direction.getAdjacentZ() * 16);
            }
        }

        double f = d;
        double g = e;
        return (f, gx, h) -> {
            return distanceToCube(f - 8.0D - f, gx - OLD_CHUNK_CENTER_Y, h - 8.0D - g, 8.0D, OLD_CHUNK_Y_RADIUS, 8.0D);
        };
    }

    private static double distanceToCube(double d, double e, double f, double g, double h, double i) {
        double j = Math.abs(d) - g;
        double k = Math.abs(e) - h;
        double l = Math.abs(f) - i;
        return MathHelper.length(Math.max(0.0D, j), Math.max(0.0D, k), Math.max(0.0D, l));
    }

    interface CellValueGetter {
        double get(BlendingData blendingData, int i, int j, int k);
    }

    public interface DistanceGetter {
        double getDistance(double d, double e, double f);
    }

    static record PositionedBlendingData(int chunkX, int chunkZ, BlendingData blendingData) {
        PositionedBlendingData(int i, int j, BlendingData blendingData) {
            this.chunkX = i;
            this.chunkZ = j;
            this.blendingData = blendingData;
        }

        public int chunkX() {
            return this.chunkX;
        }

        public int chunkZ() {
            return this.chunkZ;
        }

        public BlendingData blendingData() {
            return this.blendingData;
        }
    }
}
