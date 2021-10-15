package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class WorldGenCarverConfiguration extends WorldGenFeatureConfigurationChance {
    public static final MapCodec<WorldGenCarverConfiguration> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((carverConfiguration) -> {
            return carverConfiguration.probability;
        }), HeightProvider.CODEC.fieldOf("y").forGetter((carverConfiguration) -> {
            return carverConfiguration.y;
        }), FloatProvider.CODEC.fieldOf("yScale").forGetter((carverConfiguration) -> {
            return carverConfiguration.yScale;
        }), VerticalAnchor.CODEC.fieldOf("lava_level").forGetter((carverConfiguration) -> {
            return carverConfiguration.lavaLevel;
        }), Codec.BOOL.fieldOf("aquifers_enabled").forGetter((carverConfiguration) -> {
            return carverConfiguration.aquifersEnabled;
        }), CarverDebugSettings.CODEC.optionalFieldOf("debug_settings", CarverDebugSettings.DEFAULT).forGetter((carverConfiguration) -> {
            return carverConfiguration.debugSettings;
        })).apply(instance, WorldGenCarverConfiguration::new);
    });
    public final HeightProvider y;
    public final FloatProvider yScale;
    public final VerticalAnchor lavaLevel;
    public final boolean aquifersEnabled;
    public final CarverDebugSettings debugSettings;

    public WorldGenCarverConfiguration(float probability, HeightProvider y, FloatProvider yScale, VerticalAnchor lavaLevel, boolean aquifers, CarverDebugSettings debugConfig) {
        super(probability);
        this.y = y;
        this.yScale = yScale;
        this.lavaLevel = lavaLevel;
        this.aquifersEnabled = aquifers;
        this.debugSettings = debugConfig;
    }
}
