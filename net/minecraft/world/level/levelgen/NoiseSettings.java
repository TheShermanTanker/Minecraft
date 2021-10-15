package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionManager;

public class NoiseSettings {
    public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(DimensionManager.MIN_Y, DimensionManager.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), Codec.intRange(0, DimensionManager.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings), NoiseSlideSettings.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings), NoiseSlideSettings.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings), Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical), Codec.DOUBLE.fieldOf("density_factor").forGetter(NoiseSettings::densityFactor), Codec.DOUBLE.fieldOf("density_offset").forGetter(NoiseSettings::densityOffset), Codec.BOOL.fieldOf("simplex_surface_noise").forGetter(NoiseSettings::useSimplexSurfaceNoise), Codec.BOOL.optionalFieldOf("random_density_offset", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::randomDensityOffset), Codec.BOOL.optionalFieldOf("island_noise_override", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::islandNoiseOverride), Codec.BOOL.optionalFieldOf("amplified", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::isAmplified)).apply(instance, NoiseSettings::new);
    }).comapFlatMap(NoiseSettings::guardY, Function.identity());
    private final int minY;
    private final int height;
    private final NoiseSamplingSettings noiseSamplingSettings;
    private final NoiseSlideSettings topSlideSettings;
    private final NoiseSlideSettings bottomSlideSettings;
    private final int noiseSizeHorizontal;
    private final int noiseSizeVertical;
    private final double densityFactor;
    private final double densityOffset;
    private final boolean useSimplexSurfaceNoise;
    private final boolean randomDensityOffset;
    private final boolean islandNoiseOverride;
    private final boolean isAmplified;

    private static DataResult<NoiseSettings> guardY(NoiseSettings config) {
        if (config.minY() + config.height() > DimensionManager.MAX_Y + 1) {
            return DataResult.error("min_y + height cannot be higher than: " + (DimensionManager.MAX_Y + 1));
        } else if (config.height() % 16 != 0) {
            return DataResult.error("height has to be a multiple of 16");
        } else {
            return config.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(config);
        }
    }

    private NoiseSettings(int minimumY, int height, NoiseSamplingSettings sampling, NoiseSlideSettings topSlide, NoiseSlideSettings bottomSlide, int horizontalSize, int verticalSize, double densityFactor, double densityOffset, boolean simplexSurfaceNoise, boolean randomDensityOffset, boolean islandNoiseOverride, boolean amplified) {
        this.minY = minimumY;
        this.height = height;
        this.noiseSamplingSettings = sampling;
        this.topSlideSettings = topSlide;
        this.bottomSlideSettings = bottomSlide;
        this.noiseSizeHorizontal = horizontalSize;
        this.noiseSizeVertical = verticalSize;
        this.densityFactor = densityFactor;
        this.densityOffset = densityOffset;
        this.useSimplexSurfaceNoise = simplexSurfaceNoise;
        this.randomDensityOffset = randomDensityOffset;
        this.islandNoiseOverride = islandNoiseOverride;
        this.isAmplified = amplified;
    }

    public static NoiseSettings create(int minimumY, int height, NoiseSamplingSettings sampling, NoiseSlideSettings topSlide, NoiseSlideSettings bottomSlide, int horizontalSize, int verticalSize, double densityFactor, double densityOffset, boolean simplexSurfaceNoise, boolean randomDensityOffset, boolean islandNoiseOverride, boolean amplified) {
        NoiseSettings noiseSettings = new NoiseSettings(minimumY, height, sampling, topSlide, bottomSlide, horizontalSize, verticalSize, densityFactor, densityOffset, simplexSurfaceNoise, randomDensityOffset, islandNoiseOverride, amplified);
        guardY(noiseSettings).error().ifPresent((partialResult) -> {
            throw new IllegalStateException(partialResult.message());
        });
        return noiseSettings;
    }

    public int minY() {
        return this.minY;
    }

    public int height() {
        return this.height;
    }

    public NoiseSamplingSettings noiseSamplingSettings() {
        return this.noiseSamplingSettings;
    }

    public NoiseSlideSettings topSlideSettings() {
        return this.topSlideSettings;
    }

    public NoiseSlideSettings bottomSlideSettings() {
        return this.bottomSlideSettings;
    }

    public int noiseSizeHorizontal() {
        return this.noiseSizeHorizontal;
    }

    public int noiseSizeVertical() {
        return this.noiseSizeVertical;
    }

    public double densityFactor() {
        return this.densityFactor;
    }

    public double densityOffset() {
        return this.densityOffset;
    }

    @Deprecated
    public boolean useSimplexSurfaceNoise() {
        return this.useSimplexSurfaceNoise;
    }

    @Deprecated
    public boolean randomDensityOffset() {
        return this.randomDensityOffset;
    }

    @Deprecated
    public boolean islandNoiseOverride() {
        return this.islandNoiseOverride;
    }

    @Deprecated
    public boolean isAmplified() {
        return this.isAmplified;
    }
}
