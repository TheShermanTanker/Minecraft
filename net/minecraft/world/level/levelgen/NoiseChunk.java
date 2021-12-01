package net.minecraft.world.level.levelgen;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;

public class NoiseChunk {
    private final NoiseSampler sampler;
    final NoiseSettings noiseSettings;
    final int cellCountXZ;
    final int cellCountY;
    final int cellNoiseMinY;
    final int firstCellX;
    final int firstCellZ;
    private final int firstNoiseX;
    private final int firstNoiseZ;
    final List<NoiseChunk.NoiseInterpolator> interpolators;
    private final NoiseSampler.FlatNoiseData[][] noiseData;
    private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
    private final Aquifer aquifer;
    private final NoiseChunk.BlockStateFiller baseNoise;
    private final NoiseChunk.BlockStateFiller oreVeins;
    private final Blender blender;

    public static NoiseChunk forChunk(IChunkAccess chunk, NoiseSampler noiseSampler, Supplier<NoiseChunk.NoiseFiller> supplier, GeneratorSettingBase chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        NoiseSettings noiseSettings = chunkGeneratorSettings.noiseSettings();
        int i = Math.max(noiseSettings.minY(), chunk.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), chunk.getMaxBuildHeight());
        int k = MathHelper.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = MathHelper.intFloorDiv(j - i, noiseSettings.getCellHeight());
        return new NoiseChunk(16 / noiseSettings.getCellWidth(), l, k, noiseSampler, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(), supplier.get(), chunkGeneratorSettings, fluidLevelSampler, blender);
    }

    public static NoiseChunk forColumn(int minimumY, int i, int horizontalSize, int verticalNoiseResolution, NoiseSampler noiseSampler, GeneratorSettingBase chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler) {
        return new NoiseChunk(1, verticalNoiseResolution, horizontalSize, noiseSampler, minimumY, i, (ix, j, k) -> {
            return 0.0D;
        }, chunkGeneratorSettings, fluidLevelSampler, Blender.empty());
    }

    private NoiseChunk(int horizontalNoiseResolution, int verticalNoiseResolution, int horizontalSize, NoiseSampler noiseSampler, int minimumY, int minimumZ, NoiseChunk.NoiseFiller noiseFiller, GeneratorSettingBase chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        this.noiseSettings = chunkGeneratorSettings.noiseSettings();
        this.cellCountXZ = horizontalNoiseResolution;
        this.cellCountY = verticalNoiseResolution;
        this.cellNoiseMinY = horizontalSize;
        this.sampler = noiseSampler;
        int i = this.noiseSettings.getCellWidth();
        this.firstCellX = Math.floorDiv(minimumY, i);
        this.firstCellZ = Math.floorDiv(minimumZ, i);
        this.interpolators = Lists.newArrayList();
        this.firstNoiseX = QuartPos.fromBlock(minimumY);
        this.firstNoiseZ = QuartPos.fromBlock(minimumZ);
        int j = QuartPos.fromBlock(horizontalNoiseResolution * i);
        this.noiseData = new NoiseSampler.FlatNoiseData[j + 1][];
        this.blender = blender;

        for(int k = 0; k <= j; ++k) {
            int l = this.firstNoiseX + k;
            this.noiseData[k] = new NoiseSampler.FlatNoiseData[j + 1];

            for(int m = 0; m <= j; ++m) {
                int n = this.firstNoiseZ + m;
                this.noiseData[k][m] = noiseSampler.noiseData(l, n, blender);
            }
        }

        this.aquifer = noiseSampler.createAquifer(this, minimumY, minimumZ, horizontalSize, verticalNoiseResolution, fluidLevelSampler, chunkGeneratorSettings.isAquifersEnabled());
        this.baseNoise = noiseSampler.makeBaseNoiseFiller(this, noiseFiller, chunkGeneratorSettings.isNoodleCavesEnabled());
        this.oreVeins = noiseSampler.makeOreVeinifier(this, chunkGeneratorSettings.isOreVeinsEnabled());
    }

    public NoiseSampler.FlatNoiseData noiseData(int x, int z) {
        return this.noiseData[x - this.firstNoiseX][z - this.firstNoiseZ];
    }

    public int preliminarySurfaceLevel(int i, int j) {
        return this.preliminarySurfaceLevel.computeIfAbsent(ChunkCoordIntPair.pair(QuartPos.fromBlock(i), QuartPos.fromBlock(j)), this::computePreliminarySurfaceLevel);
    }

    private int computePreliminarySurfaceLevel(long l) {
        int i = ChunkCoordIntPair.getX(l);
        int j = ChunkCoordIntPair.getZ(l);
        int k = i - this.firstNoiseX;
        int m = j - this.firstNoiseZ;
        int n = this.noiseData.length;
        TerrainInfo terrainInfo;
        if (k >= 0 && m >= 0 && k < n && m < n) {
            terrainInfo = this.noiseData[k][m].terrainInfo();
        } else {
            terrainInfo = this.sampler.noiseData(i, j, this.blender).terrainInfo();
        }

        return this.sampler.getPreliminarySurfaceLevel(QuartPos.toBlock(i), QuartPos.toBlock(j), terrainInfo);
    }

    protected NoiseChunk.NoiseInterpolator createNoiseInterpolator(NoiseChunk.NoiseFiller columnSampler) {
        return new NoiseChunk.NoiseInterpolator(columnSampler);
    }

    public Blender getBlender() {
        return this.blender;
    }

    public void initializeForFirstCellX() {
        this.interpolators.forEach((interpolator) -> {
            interpolator.initializeForFirstCellX();
        });
    }

    public void advanceCellX(int x) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.advanceCellX(x);
        });
    }

    public void selectCellYZ(int noiseY, int noiseZ) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.selectCellYZ(noiseY, noiseZ);
        });
    }

    public void updateForY(double deltaY) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForY(deltaY);
        });
    }

    public void updateForX(double deltaX) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForX(deltaX);
        });
    }

    public void updateForZ(double deltaZ) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForZ(deltaZ);
        });
    }

    public void swapSlices() {
        this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
    }

    public Aquifer aquifer() {
        return this.aquifer;
    }

    @Nullable
    protected IBlockData updateNoiseAndGenerateBaseState(int x, int y, int z) {
        return this.baseNoise.calculate(x, y, z);
    }

    @Nullable
    protected IBlockData oreVeinify(int x, int y, int z) {
        return this.oreVeins.calculate(x, y, z);
    }

    @FunctionalInterface
    public interface BlockStateFiller {
        @Nullable
        IBlockData calculate(int x, int y, int z);
    }

    @FunctionalInterface
    public interface InterpolatableNoise {
        NoiseChunk.Sampler instantiate(NoiseChunk chunkNoiseSampler);
    }

    @FunctionalInterface
    public interface NoiseFiller {
        double calculateNoise(int x, int y, int z);
    }

    public class NoiseInterpolator implements NoiseChunk.Sampler {
        private double[][] slice0;
        private double[][] slice1;
        private final NoiseChunk.NoiseFiller noiseFiller;
        private double noise000;
        private double noise001;
        private double noise100;
        private double noise101;
        private double noise010;
        private double noise011;
        private double noise110;
        private double noise111;
        private double valueXZ00;
        private double valueXZ10;
        private double valueXZ01;
        private double valueXZ11;
        private double valueZ0;
        private double valueZ1;
        private double value;

        NoiseInterpolator(NoiseChunk.NoiseFiller columnSampler) {
            this.noiseFiller = columnSampler;
            this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            NoiseChunk.this.interpolators.add(this);
        }

        private double[][] allocateSlice(int sizeZ, int sizeX) {
            int i = sizeX + 1;
            int j = sizeZ + 1;
            double[][] ds = new double[i][j];

            for(int k = 0; k < i; ++k) {
                ds[k] = new double[j];
            }

            return ds;
        }

        void initializeForFirstCellX() {
            this.fillSlice(this.slice0, NoiseChunk.this.firstCellX);
        }

        void advanceCellX(int x) {
            this.fillSlice(this.slice1, NoiseChunk.this.firstCellX + x + 1);
        }

        private void fillSlice(double[][] buffer, int noiseX) {
            int i = NoiseChunk.this.noiseSettings.getCellWidth();
            int j = NoiseChunk.this.noiseSettings.getCellHeight();

            for(int k = 0; k < NoiseChunk.this.cellCountXZ + 1; ++k) {
                int l = NoiseChunk.this.firstCellZ + k;

                for(int m = 0; m < NoiseChunk.this.cellCountY + 1; ++m) {
                    int n = m + NoiseChunk.this.cellNoiseMinY;
                    int o = n * j;
                    double d = this.noiseFiller.calculateNoise(noiseX * i, o, l * i);
                    buffer[k][m] = d;
                }
            }

        }

        void selectCellYZ(int noiseY, int noiseZ) {
            this.noise000 = this.slice0[noiseZ][noiseY];
            this.noise001 = this.slice0[noiseZ + 1][noiseY];
            this.noise100 = this.slice1[noiseZ][noiseY];
            this.noise101 = this.slice1[noiseZ + 1][noiseY];
            this.noise010 = this.slice0[noiseZ][noiseY + 1];
            this.noise011 = this.slice0[noiseZ + 1][noiseY + 1];
            this.noise110 = this.slice1[noiseZ][noiseY + 1];
            this.noise111 = this.slice1[noiseZ + 1][noiseY + 1];
        }

        void updateForY(double deltaY) {
            this.valueXZ00 = MathHelper.lerp(deltaY, this.noise000, this.noise010);
            this.valueXZ10 = MathHelper.lerp(deltaY, this.noise100, this.noise110);
            this.valueXZ01 = MathHelper.lerp(deltaY, this.noise001, this.noise011);
            this.valueXZ11 = MathHelper.lerp(deltaY, this.noise101, this.noise111);
        }

        void updateForX(double deltaX) {
            this.valueZ0 = MathHelper.lerp(deltaX, this.valueXZ00, this.valueXZ10);
            this.valueZ1 = MathHelper.lerp(deltaX, this.valueXZ01, this.valueXZ11);
        }

        void updateForZ(double deltaZ) {
            this.value = MathHelper.lerp(deltaZ, this.valueZ0, this.valueZ1);
        }

        @Override
        public double sample() {
            return this.value;
        }

        private void swapSlices() {
            double[][] ds = this.slice0;
            this.slice0 = this.slice1;
            this.slice1 = ds;
        }
    }

    @FunctionalInterface
    public interface Sampler {
        double sample();
    }
}
