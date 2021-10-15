package net.minecraft.world.level.levelgen;

import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.biome.WorldChunkManagerTheEnd;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3Handler;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorOctaves;

public class NoiseSampler {
    private static final int OLD_CELL_COUNT_Y = 32;
    private static final float[] BIOME_WEIGHTS = SystemUtils.make(new float[25], (array) -> {
        for(int i = -2; i <= 2; ++i) {
            for(int j = -2; j <= 2; ++j) {
                float f = 10.0F / MathHelper.sqrt((float)(i * i + j * j) + 0.2F);
                array[i + 2 + (j + 2) * 5] = f;
            }
        }

    });
    private final WorldChunkManager biomeSource;
    private final int cellWidth;
    private final int cellHeight;
    private final int cellCountY;
    private final NoiseSettings noiseSettings;
    private final BlendedNoise blendedNoise;
    @Nullable
    private final NoiseGenerator3Handler islandNoise;
    private final NoiseGeneratorOctaves depthNoise;
    private final double topSlideTarget;
    private final double topSlideSize;
    private final double topSlideOffset;
    private final double bottomSlideTarget;
    private final double bottomSlideSize;
    private final double bottomSlideOffset;
    private final double dimensionDensityFactor;
    private final double dimensionDensityOffset;
    private final NoiseModifier caveNoiseModifier;

    public NoiseSampler(WorldChunkManager biomeSource, int horizontalNoiseResolution, int verticalNoiseResolution, int noiseSizeY, NoiseSettings config, BlendedNoise noise, @Nullable NoiseGenerator3Handler islandNoise, NoiseGeneratorOctaves densityNoise, NoiseModifier noiseModifier) {
        this.cellWidth = horizontalNoiseResolution;
        this.cellHeight = verticalNoiseResolution;
        this.biomeSource = biomeSource;
        this.cellCountY = noiseSizeY;
        this.noiseSettings = config;
        this.blendedNoise = noise;
        this.islandNoise = islandNoise;
        this.depthNoise = densityNoise;
        this.topSlideTarget = (double)config.topSlideSettings().target();
        this.topSlideSize = (double)config.topSlideSettings().size();
        this.topSlideOffset = (double)config.topSlideSettings().offset();
        this.bottomSlideTarget = (double)config.bottomSlideSettings().target();
        this.bottomSlideSize = (double)config.bottomSlideSettings().size();
        this.bottomSlideOffset = (double)config.bottomSlideSettings().offset();
        this.dimensionDensityFactor = config.densityFactor();
        this.dimensionDensityOffset = config.densityOffset();
        this.caveNoiseModifier = noiseModifier;
    }

    public void fillNoiseColumn(double[] buffer, int x, int z, NoiseSettings config, int seaLevel, int minY, int noiseSizeY) {
        double d;
        double e;
        if (this.islandNoise != null) {
            d = (double)(WorldChunkManagerTheEnd.getHeightValue(this.islandNoise, x, z) - 8.0F);
            if (d > 0.0D) {
                e = 0.25D;
            } else {
                e = 1.0D;
            }
        } else {
            float g = 0.0F;
            float h = 0.0F;
            float i = 0.0F;
            int j = 2;
            int k = seaLevel;
            float l = this.biomeSource.getBiome(x, seaLevel, z).getDepth();

            for(int m = -2; m <= 2; ++m) {
                for(int n = -2; n <= 2; ++n) {
                    BiomeBase biome = this.biomeSource.getBiome(x + m, k, z + n);
                    float o = biome.getDepth();
                    float p = biome.getScale();
                    float q;
                    float r;
                    if (config.isAmplified() && o > 0.0F) {
                        q = 1.0F + o * 2.0F;
                        r = 1.0F + p * 4.0F;
                    } else {
                        q = o;
                        r = p;
                    }

                    float u = o > l ? 0.5F : 1.0F;
                    float v = u * BIOME_WEIGHTS[m + 2 + (n + 2) * 5] / (q + 2.0F);
                    g += r * v;
                    h += q * v;
                    i += v;
                }
            }

            float w = h / i;
            float y = g / i;
            double aa = (double)(w * 0.5F - 0.125F);
            double ab = (double)(y * 0.9F + 0.1F);
            d = aa * 0.265625D;
            e = 96.0D / ab;
        }

        double ae = 684.412D * config.noiseSamplingSettings().xzScale();
        double af = 684.412D * config.noiseSamplingSettings().yScale();
        double ag = ae / config.noiseSamplingSettings().xzFactor();
        double ah = af / config.noiseSamplingSettings().yFactor();
        double ai = config.randomDensityOffset() ? this.getRandomDensity(x, z) : 0.0D;

        for(int aj = 0; aj <= noiseSizeY; ++aj) {
            int ak = aj + minY;
            double al = this.blendedNoise.sampleAndClampNoise(x, ak, z, ae, af, ag, ah);
            double am = this.computeInitialDensity(ak, d, e, ai) + al;
            am = this.caveNoiseModifier.modifyNoise(am, ak * this.cellHeight, z * this.cellWidth, x * this.cellWidth);
            am = this.applySlide(am, ak);
            buffer[aj] = am;
        }

    }

    private double computeInitialDensity(int y, double depth, double scale, double randomDensityOffset) {
        double d = 1.0D - (double)y * 2.0D / 32.0D + randomDensityOffset;
        double e = d * this.dimensionDensityFactor + this.dimensionDensityOffset;
        double f = (e + depth) * scale;
        return f * (double)(f > 0.0D ? 4 : 1);
    }

    private double applySlide(double noise, int y) {
        int i = MathHelper.intFloorDiv(this.noiseSettings.minY(), this.cellHeight);
        int j = y - i;
        if (this.topSlideSize > 0.0D) {
            double d = ((double)(this.cellCountY - j) - this.topSlideOffset) / this.topSlideSize;
            noise = MathHelper.clampedLerp(this.topSlideTarget, noise, d);
        }

        if (this.bottomSlideSize > 0.0D) {
            double e = ((double)j - this.bottomSlideOffset) / this.bottomSlideSize;
            noise = MathHelper.clampedLerp(this.bottomSlideTarget, noise, e);
        }

        return noise;
    }

    private double getRandomDensity(int x, int z) {
        double d = this.depthNoise.getValue((double)(x * 200), 10.0D, (double)(z * 200), 1.0D, 0.0D, true);
        double e;
        if (d < 0.0D) {
            e = -d * 0.3D;
        } else {
            e = d;
        }

        double g = e * 24.575625D - 2.0D;
        return g < 0.0D ? g * 0.009486607142857142D : Math.min(g, 1.0D) * 0.006640625D;
    }
}
