package net.minecraft.world.level.levelgen.synth;

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

    public static NoiseGeneratorNormal create(RandomSource random, int offset, double... octaves) {
        return new NoiseGeneratorNormal(random, offset, new DoubleArrayList(octaves));
    }

    public static NoiseGeneratorNormal create(RandomSource random, int offset, DoubleList octaves) {
        return new NoiseGeneratorNormal(random, offset, octaves);
    }

    private NoiseGeneratorNormal(RandomSource random, int offset, DoubleList octaves) {
        this.first = NoiseGeneratorOctaves.create(random, offset, octaves);
        this.second = NoiseGeneratorOctaves.create(random, offset, octaves);
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
}
