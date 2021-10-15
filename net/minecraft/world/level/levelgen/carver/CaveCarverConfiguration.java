package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CaveCarverConfiguration extends WorldGenCarverConfiguration {
    public static final Codec<CaveCarverConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenCarverConfiguration.CODEC.forGetter((caveCarverConfiguration) -> {
            return caveCarverConfiguration;
        }), FloatProvider.CODEC.fieldOf("horizontal_radius_multiplier").forGetter((caveCarverConfiguration) -> {
            return caveCarverConfiguration.horizontalRadiusMultiplier;
        }), FloatProvider.CODEC.fieldOf("vertical_radius_multiplier").forGetter((caveCarverConfiguration) -> {
            return caveCarverConfiguration.verticalRadiusMultiplier;
        }), FloatProvider.codec(-1.0F, 1.0F).fieldOf("floor_level").forGetter((caveCarverConfiguration) -> {
            return caveCarverConfiguration.floorLevel;
        })).apply(instance, CaveCarverConfiguration::new);
    });
    public final FloatProvider horizontalRadiusMultiplier;
    public final FloatProvider verticalRadiusMultiplier;
    final FloatProvider floorLevel;

    public CaveCarverConfiguration(float probability, HeightProvider y, FloatProvider yScale, VerticalAnchor lavaLevel, boolean aquifers, CarverDebugSettings debugConfig, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
        super(probability, y, yScale, lavaLevel, aquifers, debugConfig);
        this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = verticalRadiusMultiplier;
        this.floorLevel = floorLevel;
    }

    public CaveCarverConfiguration(float probability, HeightProvider y, FloatProvider yScale, VerticalAnchor lavaLevel, boolean aquifers, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
        this(probability, y, yScale, lavaLevel, aquifers, CarverDebugSettings.DEFAULT, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
    }

    public CaveCarverConfiguration(WorldGenCarverConfiguration config, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
        this(config.probability, config.y, config.yScale, config.lavaLevel, config.aquifersEnabled, config.debugSettings, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
    }
}
