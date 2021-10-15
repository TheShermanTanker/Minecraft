package net.minecraft.world.level.levelgen;

import java.util.Random;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;

public class NoodleCavifier {
    private static final int NOODLES_MAX_Y = 30;
    private static final double SPACING_AND_STRAIGHTNESS = 1.5D;
    private static final double XZ_FREQUENCY = 2.6666666666666665D;
    private static final double Y_FREQUENCY = 2.6666666666666665D;
    private final NoiseGeneratorNormal toggleNoiseSource;
    private final NoiseGeneratorNormal thicknessNoiseSource;
    private final NoiseGeneratorNormal noodleANoiseSource;
    private final NoiseGeneratorNormal noodleBNoiseSource;

    public NoodleCavifier(long seed) {
        Random random = new Random(seed);
        this.toggleNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.thicknessNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.noodleANoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.noodleBNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
    }

    public void fillToggleNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, minY, noiseSizeY, this.toggleNoiseSource, 1.0D);
    }

    public void fillThicknessNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, minY, noiseSizeY, this.thicknessNoiseSource, 1.0D);
    }

    public void fillRidgeANoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, minY, noiseSizeY, this.noodleANoiseSource, 2.6666666666666665D, 2.6666666666666665D);
    }

    public void fillRidgeBNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, minY, noiseSizeY, this.noodleBNoiseSource, 2.6666666666666665D, 2.6666666666666665D);
    }

    public void fillNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY, NoiseGeneratorNormal sampler, double scale) {
        this.fillNoiseColumn(buffer, x, z, minY, noiseSizeY, sampler, scale, scale);
    }

    public void fillNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY, NoiseGeneratorNormal sampler, double horizontalScale, double verticalScale) {
        int i = 8;
        int j = 4;

        for(int k = 0; k < noiseSizeY; ++k) {
            int l = k + minY;
            int m = x * 4;
            int n = l * 8;
            int o = z * 4;
            double d;
            if (n < 38) {
                d = NoiseUtils.sampleNoiseAndMapToRange(sampler, (double)m * horizontalScale, (double)n * verticalScale, (double)o * horizontalScale, -1.0D, 1.0D);
            } else {
                d = 1.0D;
            }

            buffer[k] = d;
        }

    }

    public double noodleCavify(double weight, int x, int y, int z, double frequencyNoise, double weightReducingNoise, double firstWeightNoise, double secondWeightNoise, int minY) {
        if (y <= 30 && y >= minY + 4) {
            if (weight < 0.0D) {
                return weight;
            } else if (frequencyNoise < 0.0D) {
                return weight;
            } else {
                double d = 0.05D;
                double e = 0.1D;
                double f = MathHelper.clampedMap(weightReducingNoise, -1.0D, 1.0D, 0.05D, 0.1D);
                double g = Math.abs(1.5D * firstWeightNoise) - f;
                double h = Math.abs(1.5D * secondWeightNoise) - f;
                double i = Math.max(g, h);
                return Math.min(weight, i);
            }
        } else {
            return weight;
        }
    }
}
