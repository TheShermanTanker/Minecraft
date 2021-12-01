package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import net.minecraft.world.level.levelgen.RandomSource;

public class NoiseGeneratorNormal {
    private static final double INPUT_FACTOR = 1.0181268882175227D;
    private static final double TARGET_DEVIATION = 0.3333333333333333D;
    private final double valueFactor;
    private final NoiseGeneratorOctaves first;
    private final NoiseGeneratorOctaves second;

    /** @deprecated */
    @Deprecated
    public static NoiseGeneratorNormal createLegacyNetherBiome(RandomSource random, NormalNoise$NoiseParameters parameters) {
        return new NoiseGeneratorNormal(random, parameters.firstOctave(), parameters.amplitudes(), false);
    }

    public static NoiseGeneratorNormal create(RandomSource random, int offset, double... octaves) {
        return new NoiseGeneratorNormal(random, offset, new DoubleArrayList(octaves), true);
    }

    public static NoiseGeneratorNormal create(RandomSource random, NormalNoise$NoiseParameters parameters) {
        return new NoiseGeneratorNormal(random, parameters.firstOctave(), parameters.amplitudes(), true);
    }

    public static NoiseGeneratorNormal create(RandomSource random, int offset, DoubleList octaves) {
        return new NoiseGeneratorNormal(random, offset, octaves, true);
    }

    private NoiseGeneratorNormal(RandomSource random, int offset, DoubleList octaves, boolean xoroshiro) {
        if (xoroshiro) {
            this.first = NoiseGeneratorOctaves.create(random, offset, octaves);
            this.second = NoiseGeneratorOctaves.create(random, offset, octaves);
        } else {
            this.first = NoiseGeneratorOctaves.createLegacyForLegacyNormalNoise(random, offset, octaves);
            this.second = NoiseGeneratorOctaves.createLegacyForLegacyNormalNoise(random, offset, octaves);
        }

        int i = Integer.MAX_VALUE;
        int j = Integer.MIN_VALUE;
        DoubleListIterator doubleListIterator = octaves.iterator();

        while(doubleListIterator.hasNext()) {
            int k = doubleListIterator.nextIndex();
            double d = doubleListIterator.nextDouble();
            if (d != 0.0D) {
                i = Math.min(i, k);
                j = Math.max(j, k);
            }
        }

        this.valueFactor = 0.16666666666666666D / expectedDeviation(j - i);
    }

    private static double expectedDeviation(int octaves) {
        return 0.1D * (1.0D + 1.0D / (double)(octaves + 1));
    }

    public double getValue(double x, double y, double z) {
        double d = x * 1.0181268882175227D;
        double e = y * 1.0181268882175227D;
        double f = z * 1.0181268882175227D;
        return (this.first.getValue(x, y, z) + this.second.getValue(d, e, f)) * this.valueFactor;
    }

    public NormalNoise$NoiseParameters parameters() {
        return new NormalNoise$NoiseParameters(this.first.firstOctave(), this.first.amplitudes());
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder info) {
        info.append("NormalNoise {");
        info.append("first: ");
        this.first.parityConfigString(info);
        info.append(", second: ");
        this.second.parityConfigString(info);
        info.append("}");
    }
}
