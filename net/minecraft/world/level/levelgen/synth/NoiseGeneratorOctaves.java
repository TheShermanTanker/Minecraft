package net.minecraft.world.level.levelgen.synth;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;

public class NoiseGeneratorOctaves implements NoiseGenerator {
    private static final int ROUND_OFF = 33554432;
    private final NoiseGeneratorPerlin[] noiseLevels;
    private final DoubleList amplitudes;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;

    public NoiseGeneratorOctaves(RandomSource random, IntStream octaves) {
        this(random, octaves.boxed().collect(ImmutableList.toImmutableList()));
    }

    public NoiseGeneratorOctaves(RandomSource random, List<Integer> octaves) {
        this(random, new IntRBTreeSet(octaves));
    }

    public static NoiseGeneratorOctaves create(RandomSource random, int offset, double... amplitudes) {
        return create(random, offset, (DoubleList)(new DoubleArrayList(amplitudes)));
    }

    public static NoiseGeneratorOctaves create(RandomSource random, int offset, DoubleList amplitudes) {
        return new NoiseGeneratorOctaves(random, Pair.of(offset, amplitudes));
    }

    private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet octaves) {
        if (octaves.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        } else {
            int i = -octaves.firstInt();
            int j = octaves.lastInt();
            int k = i + j + 1;
            if (k < 1) {
                throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
            } else {
                DoubleList doubleList = new DoubleArrayList(new double[k]);
                IntBidirectionalIterator intBidirectionalIterator = octaves.iterator();

                while(intBidirectionalIterator.hasNext()) {
                    int l = intBidirectionalIterator.nextInt();
                    doubleList.set(l + i, 1.0D);
                }

                return Pair.of(-i, doubleList);
            }
        }
    }

    private NoiseGeneratorOctaves(RandomSource random, IntSortedSet octaves) {
        this(random, octaves, SeededRandom::new);
    }

    private NoiseGeneratorOctaves(RandomSource random, IntSortedSet octaves, LongFunction<RandomSource> randomFunction) {
        this(random, makeAmplitudes(octaves), randomFunction);
    }

    protected NoiseGeneratorOctaves(RandomSource random, Pair<Integer, DoubleList> offsetAndAmplitudes) {
        this(random, offsetAndAmplitudes, SeededRandom::new);
    }

    protected NoiseGeneratorOctaves(RandomSource random, Pair<Integer, DoubleList> octaves, LongFunction<RandomSource> randomFunction) {
        int i = octaves.getFirst();
        this.amplitudes = octaves.getSecond();
        NoiseGeneratorPerlin improvedNoise = new NoiseGeneratorPerlin(random);
        int j = this.amplitudes.size();
        int k = -i;
        this.noiseLevels = new NoiseGeneratorPerlin[j];
        if (k >= 0 && k < j) {
            double d = this.amplitudes.getDouble(k);
            if (d != 0.0D) {
                this.noiseLevels[k] = improvedNoise;
            }
        }

        for(int l = k - 1; l >= 0; --l) {
            if (l < j) {
                double e = this.amplitudes.getDouble(l);
                if (e != 0.0D) {
                    this.noiseLevels[l] = new NoiseGeneratorPerlin(random);
                } else {
                    skipOctave(random);
                }
            } else {
                skipOctave(random);
            }
        }

        if (k < j - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
        } else {
            this.lowestFreqInputFactor = Math.pow(2.0D, (double)(-k));
            this.lowestFreqValueFactor = Math.pow(2.0D, (double)(j - 1)) / (Math.pow(2.0D, (double)j) - 1.0D);
        }
    }

    private static void skipOctave(RandomSource random) {
        random.consumeCount(262);
    }

    public double getValue(double x, double y, double z) {
        return this.getValue(x, y, z, 0.0D, 0.0D, false);
    }

    @Deprecated
    public double getValue(double x, double y, double z, double yScale, double yMax, boolean useOrigin) {
        double d = 0.0D;
        double e = this.lowestFreqInputFactor;
        double f = this.lowestFreqValueFactor;

        for(int i = 0; i < this.noiseLevels.length; ++i) {
            NoiseGeneratorPerlin improvedNoise = this.noiseLevels[i];
            if (improvedNoise != null) {
                double g = improvedNoise.noise(wrap(x * e), useOrigin ? -improvedNoise.yo : wrap(y * e), wrap(z * e), yScale * e, yMax * e);
                d += this.amplitudes.getDouble(i) * g * f;
            }

            e *= 2.0D;
            f /= 2.0D;
        }

        return d;
    }

    @Nullable
    public NoiseGeneratorPerlin getOctaveNoise(int octave) {
        return this.noiseLevels[this.noiseLevels.length - 1 - octave];
    }

    public static double wrap(double value) {
        return value - (double)MathHelper.lfloor(value / 3.3554432E7D + 0.5D) * 3.3554432E7D;
    }

    @Override
    public double getSurfaceNoiseValue(double x, double y, double yScale, double yMax) {
        return this.getValue(x, y, 0.0D, yScale, yMax, false);
    }
}
