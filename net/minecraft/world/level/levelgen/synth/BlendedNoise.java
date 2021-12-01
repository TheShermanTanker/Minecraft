package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import java.util.stream.IntStream;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.RandomSource;

public class BlendedNoise implements NoiseChunk.NoiseFiller {
    private final NoiseGeneratorOctaves minLimitNoise;
    private final NoiseGeneratorOctaves maxLimitNoise;
    private final NoiseGeneratorOctaves mainNoise;
    private final double xzScale;
    private final double yScale;
    private final double xzMainScale;
    private final double yMainScale;
    private final int cellWidth;
    private final int cellHeight;

    private BlendedNoise(NoiseGeneratorOctaves lowerInterpolatedNoise, NoiseGeneratorOctaves upperInterpolatedNoise, NoiseGeneratorOctaves interpolationNoise, NoiseSamplingSettings config, int cellWidth, int cellHeight) {
        this.minLimitNoise = lowerInterpolatedNoise;
        this.maxLimitNoise = upperInterpolatedNoise;
        this.mainNoise = interpolationNoise;
        this.xzScale = 684.412D * config.xzScale();
        this.yScale = 684.412D * config.yScale();
        this.xzMainScale = this.xzScale / config.xzFactor();
        this.yMainScale = this.yScale / config.yFactor();
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public BlendedNoise(RandomSource random, NoiseSamplingSettings config, int cellWidth, int cellHeight) {
        this(NoiseGeneratorOctaves.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), NoiseGeneratorOctaves.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), NoiseGeneratorOctaves.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)), config, cellWidth, cellHeight);
    }

    @Override
    public double calculateNoise(int x, int y, int z) {
        int i = Math.floorDiv(x, this.cellWidth);
        int j = Math.floorDiv(y, this.cellHeight);
        int k = Math.floorDiv(z, this.cellWidth);
        double d = 0.0D;
        double e = 0.0D;
        double f = 0.0D;
        boolean bl = true;
        double g = 1.0D;

        for(int l = 0; l < 8; ++l) {
            NoiseGeneratorPerlin improvedNoise = this.mainNoise.getOctaveNoise(l);
            if (improvedNoise != null) {
                f += improvedNoise.noise(NoiseGeneratorOctaves.wrap((double)i * this.xzMainScale * g), NoiseGeneratorOctaves.wrap((double)j * this.yMainScale * g), NoiseGeneratorOctaves.wrap((double)k * this.xzMainScale * g), this.yMainScale * g, (double)j * this.yMainScale * g) / g;
            }

            g /= 2.0D;
        }

        double h = (f / 10.0D + 1.0D) / 2.0D;
        boolean bl2 = h >= 1.0D;
        boolean bl3 = h <= 0.0D;
        g = 1.0D;

        for(int m = 0; m < 16; ++m) {
            double n = NoiseGeneratorOctaves.wrap((double)i * this.xzScale * g);
            double o = NoiseGeneratorOctaves.wrap((double)j * this.yScale * g);
            double p = NoiseGeneratorOctaves.wrap((double)k * this.xzScale * g);
            double q = this.yScale * g;
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

        return MathHelper.clampedLerp(d / 512.0D, e / 512.0D, h) / 128.0D;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder info) {
        info.append("BlendedNoise{minLimitNoise=");
        this.minLimitNoise.parityConfigString(info);
        info.append(", maxLimitNoise=");
        this.maxLimitNoise.parityConfigString(info);
        info.append(", mainNoise=");
        this.mainNoise.parityConfigString(info);
        info.append(String.format(", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=%d, cellHeight=%d", this.xzScale, this.yScale, this.xzMainScale, this.yMainScale, this.cellWidth, this.cellHeight)).append('}');
    }
}
