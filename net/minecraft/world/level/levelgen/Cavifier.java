package net.minecraft.world.level.levelgen;

import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;

public class Cavifier implements NoiseModifier {
    private final int minCellY;
    private final NoiseGeneratorNormal layerNoiseSource;
    private final NoiseGeneratorNormal pillarNoiseSource;
    private final NoiseGeneratorNormal pillarRarenessModulator;
    private final NoiseGeneratorNormal pillarThicknessModulator;
    private final NoiseGeneratorNormal spaghetti2dNoiseSource;
    private final NoiseGeneratorNormal spaghetti2dElevationModulator;
    private final NoiseGeneratorNormal spaghetti2dRarityModulator;
    private final NoiseGeneratorNormal spaghetti2dThicknessModulator;
    private final NoiseGeneratorNormal spaghetti3dNoiseSource1;
    private final NoiseGeneratorNormal spaghetti3dNoiseSource2;
    private final NoiseGeneratorNormal spaghetti3dRarityModulator;
    private final NoiseGeneratorNormal spaghetti3dThicknessModulator;
    private final NoiseGeneratorNormal spaghettiRoughnessNoise;
    private final NoiseGeneratorNormal spaghettiRoughnessModulator;
    private final NoiseGeneratorNormal caveEntranceNoiseSource;
    private final NoiseGeneratorNormal cheeseNoiseSource;
    private static final int CHEESE_NOISE_RANGE = 128;
    private static final int SURFACE_DENSITY_THRESHOLD = 170;

    public Cavifier(RandomSource random, int minY) {
        this.minCellY = minY;
        this.pillarNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D, 1.0D);
        this.pillarRarenessModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.pillarThicknessModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.spaghetti2dNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.spaghetti2dElevationModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.spaghetti2dRarityModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -11, 1.0D);
        this.spaghetti2dThicknessModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -11, 1.0D);
        this.spaghetti3dNoiseSource1 = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.spaghetti3dNoiseSource2 = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.spaghetti3dRarityModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -11, 1.0D);
        this.spaghetti3dThicknessModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.spaghettiRoughnessNoise = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -5, 1.0D);
        this.spaghettiRoughnessModulator = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.caveEntranceNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D, 1.0D, 1.0D);
        this.layerNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.cheeseNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D);
    }

    @Override
    public double modifyNoise(double weight, int x, int y, int z) {
        boolean bl = weight < 170.0D;
        double d = this.spaghettiRoughness(z, x, y);
        double e = this.getSpaghetti3d(z, x, y);
        if (bl) {
            return Math.min(weight, (e + d) * 128.0D * 5.0D);
        } else {
            double f = this.cheeseNoiseSource.getValue((double)z, (double)x / 1.5D, (double)y);
            double g = MathHelper.clamp(f + 0.25D, -1.0D, 1.0D);
            double h = (double)((float)(30 - x) / 8.0F);
            double i = g + MathHelper.clampedLerp(0.5D, 0.0D, h);
            double j = this.getLayerizedCaverns(z, x, y);
            double k = this.getSpaghetti2d(z, x, y);
            double l = i + j;
            double m = Math.min(l, Math.min(e, k) + d);
            double n = Math.max(m, this.getPillars(z, x, y));
            return 128.0D * MathHelper.clamp(n, -1.0D, 1.0D);
        }
    }

    private double addEntrances(double d, int i, int j, int k) {
        double e = this.caveEntranceNoiseSource.getValue((double)(i * 2), (double)j, (double)(k * 2));
        e = NoiseUtils.biasTowardsExtreme(e, 1.0D);
        int l = 0;
        double f = (double)(j - 0) / 40.0D;
        e = e + MathHelper.clampedLerp(0.5D, d, f);
        double g = 3.0D;
        e = 4.0D * e + 3.0D;
        return Math.min(d, e);
    }

    private double getPillars(int x, int y, int z) {
        double d = 0.0D;
        double e = 2.0D;
        double f = NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, (double)x, (double)y, (double)z, 0.0D, 2.0D);
        double g = 0.0D;
        double h = 1.1D;
        double i = NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, (double)x, (double)y, (double)z, 0.0D, 1.1D);
        i = Math.pow(i, 3.0D);
        double j = 25.0D;
        double k = 0.3D;
        double l = this.pillarNoiseSource.getValue((double)x * 25.0D, (double)y * 0.3D, (double)z * 25.0D);
        l = i * (l * 2.0D - f);
        return l > 0.03D ? l : Double.NEGATIVE_INFINITY;
    }

    private double getLayerizedCaverns(int x, int y, int z) {
        double d = this.layerNoiseSource.getValue((double)x, (double)(y * 8), (double)z);
        return MathHelper.square(d) * 4.0D;
    }

    private double getSpaghetti3d(int x, int y, int z) {
        double d = this.spaghetti3dRarityModulator.getValue((double)(x * 2), (double)y, (double)(z * 2));
        double e = Cavifier.QuantizedSpaghettiRarity.getSpaghettiRarity3D(d);
        double f = 0.065D;
        double g = 0.088D;
        double h = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3dThicknessModulator, (double)x, (double)y, (double)z, 0.065D, 0.088D);
        double i = sampleWithRarity(this.spaghetti3dNoiseSource1, (double)x, (double)y, (double)z, e);
        double j = Math.abs(e * i) - h;
        double k = sampleWithRarity(this.spaghetti3dNoiseSource2, (double)x, (double)y, (double)z, e);
        double l = Math.abs(e * k) - h;
        return clampToUnit(Math.max(j, l));
    }

    private double getSpaghetti2d(int x, int y, int z) {
        double d = this.spaghetti2dRarityModulator.getValue((double)(x * 2), (double)y, (double)(z * 2));
        double e = Cavifier.QuantizedSpaghettiRarity.getSphaghettiRarity2D(d);
        double f = 0.6D;
        double g = 1.3D;
        double h = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dThicknessModulator, (double)(x * 2), (double)y, (double)(z * 2), 0.6D, 1.3D);
        double i = sampleWithRarity(this.spaghetti2dNoiseSource, (double)x, (double)y, (double)z, e);
        double j = 0.083D;
        double k = Math.abs(e * i) - 0.083D * h;
        int l = this.minCellY;
        int m = 8;
        double n = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dElevationModulator, (double)x, 0.0D, (double)z, (double)l, 8.0D);
        double o = Math.abs(n - (double)y / 8.0D) - 1.0D * h;
        o = o * o * o;
        return clampToUnit(Math.max(o, k));
    }

    private double spaghettiRoughness(int x, int y, int z) {
        double d = NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, (double)x, (double)y, (double)z, 0.0D, 0.1D);
        return (0.4D - Math.abs(this.spaghettiRoughnessNoise.getValue((double)x, (double)y, (double)z))) * d;
    }

    private static double clampToUnit(double value) {
        return MathHelper.clamp(value, -1.0D, 1.0D);
    }

    private static double sampleWithRarity(NoiseGeneratorNormal sampler, double x, double y, double z, double scale) {
        return sampler.getValue(x / scale, y / scale, z / scale);
    }

    static final class QuantizedSpaghettiRarity {
        private QuantizedSpaghettiRarity() {
        }

        static double getSphaghettiRarity2D(double value) {
            if (value < -0.75D) {
                return 0.5D;
            } else if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.5D) {
                return 1.0D;
            } else {
                return value < 0.75D ? 2.0D : 3.0D;
            }
        }

        static double getSpaghettiRarity3D(double value) {
            if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.0D) {
                return 1.0D;
            } else {
                return value < 0.5D ? 1.5D : 2.0D;
            }
        }
    }
}
