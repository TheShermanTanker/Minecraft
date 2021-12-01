package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.dimension.DimensionManager;

public record NoiseSettings(int minY, int height, NoiseSamplingSettings noiseSamplingSettings, NoiseSlider topSlideSettings, NoiseSlider bottomSlideSettings, int noiseSizeHorizontal, int noiseSizeVertical, boolean islandNoiseOverride, boolean isAmplified, boolean largeBiomes, TerrainShaper terrainShaper) {
    public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(DimensionManager.MIN_Y, DimensionManager.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), Codec.intRange(0, DimensionManager.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings), NoiseSlider.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings), NoiseSlider.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings), Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical), Codec.BOOL.optionalFieldOf("island_noise_override", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::islandNoiseOverride), Codec.BOOL.optionalFieldOf("amplified", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::isAmplified), Codec.BOOL.optionalFieldOf("large_biomes", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::largeBiomes), TerrainShaper.CODEC.fieldOf("terrain_shaper").forGetter(NoiseSettings::terrainShaper)).apply(instance, NoiseSettings::new);
    }).comapFlatMap(NoiseSettings::guardY, Function.identity());

    public NoiseSettings(int minimumY, int height, NoiseSamplingSettings sampling, NoiseSlider topSlide, NoiseSlider bottomSlide, int horizontalSize, int verticalSize, boolean bl, boolean bl2, boolean bl3, TerrainShaper terrainShaper) {
        this.minY = minimumY;
        this.height = height;
        this.noiseSamplingSettings = sampling;
        this.topSlideSettings = topSlide;
        this.bottomSlideSettings = bottomSlide;
        this.noiseSizeHorizontal = horizontalSize;
        this.noiseSizeVertical = verticalSize;
        this.islandNoiseOverride = bl;
        this.isAmplified = bl2;
        this.largeBiomes = bl3;
        this.terrainShaper = terrainShaper;
    }

    private static DataResult<NoiseSettings> guardY(NoiseSettings config) {
        if (config.minY() + config.height() > DimensionManager.MAX_Y + 1) {
            return DataResult.error("min_y + height cannot be higher than: " + (DimensionManager.MAX_Y + 1));
        } else if (config.height() % 16 != 0) {
            return DataResult.error("height has to be a multiple of 16");
        } else {
            return config.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(config);
        }
    }

    public static NoiseSettings create(int minimumY, int height, NoiseSamplingSettings sampling, NoiseSlider topSlide, NoiseSlider bottomSlide, int horizontalSize, int verticalSize, boolean islandNoiseOverride, boolean amplified, boolean largeBiomes, TerrainShaper terrainParameters) {
        NoiseSettings noiseSettings = new NoiseSettings(minimumY, height, sampling, topSlide, bottomSlide, horizontalSize, verticalSize, islandNoiseOverride, amplified, largeBiomes, terrainParameters);
        guardY(noiseSettings).error().ifPresent((partialResult) -> {
            throw new IllegalStateException(partialResult.message());
        });
        return noiseSettings;
    }

    /** @deprecated */
    @Deprecated
    public boolean islandNoiseOverride() {
        return this.islandNoiseOverride;
    }

    /** @deprecated */
    @Deprecated
    public boolean isAmplified() {
        return this.isAmplified;
    }

    /** @deprecated */
    @Deprecated
    public boolean largeBiomes() {
        return this.largeBiomes;
    }

    public int getCellHeight() {
        return QuartPos.toBlock(this.noiseSizeVertical());
    }

    public int getCellWidth() {
        return QuartPos.toBlock(this.noiseSizeHorizontal());
    }

    public int getCellCountY() {
        return this.height() / this.getCellHeight();
    }

    public int getMinCellY() {
        return MathHelper.intFloorDiv(this.minY(), this.getCellHeight());
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

    public NoiseSlider topSlideSettings() {
        return this.topSlideSettings;
    }

    public NoiseSlider bottomSlideSettings() {
        return this.bottomSlideSettings;
    }

    public int noiseSizeHorizontal() {
        return this.noiseSizeHorizontal;
    }

    public int noiseSizeVertical() {
        return this.noiseSizeVertical;
    }

    public TerrainShaper terrainShaper() {
        return this.terrainShaper;
    }
}
