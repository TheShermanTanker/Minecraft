package net.minecraft.world.level.levelgen;

import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;

public class NoiseInterpolator {
    private double[][] slice0;
    private double[][] slice1;
    private final int cellCountY;
    private final int cellCountZ;
    private final int cellNoiseMinY;
    private final NoiseInterpolator.NoiseColumnFiller noiseColumnFiller;
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
    private final int firstCellXInChunk;
    private final int firstCellZInChunk;

    public NoiseInterpolator(int sizeX, int sizeY, int sizeZ, ChunkCoordIntPair pos, int minY, NoiseInterpolator.NoiseColumnFiller columnSampler) {
        this.cellCountY = sizeY;
        this.cellCountZ = sizeZ;
        this.cellNoiseMinY = minY;
        this.noiseColumnFiller = columnSampler;
        this.slice0 = allocateSlice(sizeY, sizeZ);
        this.slice1 = allocateSlice(sizeY, sizeZ);
        this.firstCellXInChunk = pos.x * sizeX;
        this.firstCellZInChunk = pos.z * sizeZ;
    }

    private static double[][] allocateSlice(int sizeY, int sizeZ) {
        int i = sizeZ + 1;
        int j = sizeY + 1;
        double[][] ds = new double[i][j];

        for(int k = 0; k < i; ++k) {
            ds[k] = new double[j];
        }

        return ds;
    }

    public void initializeForFirstCellX() {
        this.fillSlice(this.slice0, this.firstCellXInChunk);
    }

    public void advanceCellX(int x) {
        this.fillSlice(this.slice1, this.firstCellXInChunk + x + 1);
    }

    private void fillSlice(double[][] buffer, int noiseX) {
        for(int i = 0; i < this.cellCountZ + 1; ++i) {
            int j = this.firstCellZInChunk + i;
            this.noiseColumnFiller.fillNoiseColumn(buffer[i], noiseX, j, this.cellNoiseMinY, this.cellCountY);
        }

    }

    public void selectCellYZ(int noiseY, int noiseZ) {
        this.noise000 = this.slice0[noiseZ][noiseY];
        this.noise001 = this.slice0[noiseZ + 1][noiseY];
        this.noise100 = this.slice1[noiseZ][noiseY];
        this.noise101 = this.slice1[noiseZ + 1][noiseY];
        this.noise010 = this.slice0[noiseZ][noiseY + 1];
        this.noise011 = this.slice0[noiseZ + 1][noiseY + 1];
        this.noise110 = this.slice1[noiseZ][noiseY + 1];
        this.noise111 = this.slice1[noiseZ + 1][noiseY + 1];
    }

    public void updateForY(double deltaY) {
        this.valueXZ00 = MathHelper.lerp(deltaY, this.noise000, this.noise010);
        this.valueXZ10 = MathHelper.lerp(deltaY, this.noise100, this.noise110);
        this.valueXZ01 = MathHelper.lerp(deltaY, this.noise001, this.noise011);
        this.valueXZ11 = MathHelper.lerp(deltaY, this.noise101, this.noise111);
    }

    public void updateForX(double deltaX) {
        this.valueZ0 = MathHelper.lerp(deltaX, this.valueXZ00, this.valueXZ10);
        this.valueZ1 = MathHelper.lerp(deltaX, this.valueXZ01, this.valueXZ11);
    }

    public double calculateValue(double deltaZ) {
        return MathHelper.lerp(deltaZ, this.valueZ0, this.valueZ1);
    }

    public void swapSlices() {
        double[][] ds = this.slice0;
        this.slice0 = this.slice1;
        this.slice1 = ds;
    }

    @FunctionalInterface
    public interface NoiseColumnFiller {
        void fillNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY);
    }
}
