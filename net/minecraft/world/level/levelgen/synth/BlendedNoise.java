package net.minecraft.world.level.levelgen.synth;

import java.util.stream.IntStream;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.RandomSource;

public class BlendedNoise {
    private final NoiseGeneratorOctaves minLimitNoise;
    private final NoiseGeneratorOctaves maxLimitNoise;
    private final NoiseGeneratorOctaves mainNoise;

    public BlendedNoise(NoiseGeneratorOctaves lowerInterpolatedNoise, NoiseGeneratorOctaves upperInterpolatedNoise, NoiseGeneratorOctaves interpolationNoise) {
        this.minLimitNoise = lowerInterpolatedNoise;
        this.maxLimitNoise = upperInterpolatedNoise;
        this.mainNoise = interpolationNoise;
    }

    public BlendedNoise(RandomSource random) {
        this(new NoiseGeneratorOctaves(random, IntStream.rangeClosed(-15, 0)), new NoiseGeneratorOctaves(random, IntStream.rangeClosed(-15, 0)), new NoiseGeneratorOctaves(random, IntStream.rangeClosed(-7, 0)));
    }

    public double sampleAndClampNoise(int i, int j, int k, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
        double d = 0.0D;
        double e = 0.0D;
        double f = 0.0D;
        boolean bl = true;
        double g = 1.0D;

        for(int l = 0; l < 8; ++l) {
            NoiseGeneratorPerlin improvedNoise = this.mainNoise.getOctaveNoise(l);
            if (improvedNoise != null) {
                f += improvedNoise.noise(NoiseGeneratorOctaves.wrap((double)i * horizontalStretch * g), NoiseGeneratorOctaves.wrap((double)j * verticalStretch * g), NoiseGeneratorOctaves.wrap((double)k * horizontalStretch * g), verticalStretch * g, (double)j * verticalStretch * g) / g;
            }

            g /= 2.0D;
        }

        double h = (f / 10.0D + 1.0D) / 2.0D;
        boolean bl2 = h >= 1.0D;
        boolean bl3 = h <= 0.0D;
        g = 1.0D;

        for(int m = 0; m < 16; ++m) {
            double n = NoiseGeneratorOctaves.wrap((double)i * horizontalScale * g);
            double o = NoiseGeneratorOctaves.wrap((double)j * verticalScale * g);
            double p = NoiseGeneratorOctaves.wrap((double)k * horizontalScale * g);
            double q = verticalScale * g;
            if (!bl2) {
                NoiseGeneratorPerlin improvedNoise2 = this.minLimitNoise.getOctaveNoise(m);
                if (improvedNoise2 != null) {
                    d += improvedNoise2.noise(n, o, p, q, (double)j * q) / g;
                }
            }

            if (!bl3) {
                NoiseGeneratorPerlin improvedNoise3 = this.maxLimitNoise.getOctaveNoise(m);
                if (improvedNoise3 != null) {
                    e += improvedNoise3.noise(n, o, p, q, (double)j * q) / g;
                }
            }

            g /= 2.0D;
        }

        return MathHelper.clampedLerp(d / 512.0D, e / 512.0D, h);
    }
}
