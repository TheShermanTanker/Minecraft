package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

public class DripstoneClusterConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<DripstoneClusterConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.floorToCeilingSearchRange;
        }), IntProvider.codec(1, 128).fieldOf("height").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.height;
        }), IntProvider.codec(1, 128).fieldOf("radius").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.radius;
        }), Codec.intRange(0, 64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.maxStalagmiteStalactiteHeightDiff;
        }), Codec.intRange(1, 64).fieldOf("height_deviation").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.heightDeviation;
        }), IntProvider.codec(0, 128).fieldOf("dripstone_block_layer_thickness").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.dripstoneBlockLayerThickness;
        }), FloatProvider.codec(0.0F, 2.0F).fieldOf("density").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.density;
        }), FloatProvider.codec(0.0F, 2.0F).fieldOf("wetness").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.wetness;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_dripstone_column_at_max_distance_from_center").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.chanceOfDripstoneColumnAtMaxDistanceFromCenter;
        }), Codec.intRange(1, 64).fieldOf("max_distance_from_edge_affecting_chance_of_dripstone_column").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn;
        }), Codec.intRange(1, 64).fieldOf("max_distance_from_center_affecting_height_bias").forGetter((dripstoneClusterConfiguration) -> {
            return dripstoneClusterConfiguration.maxDistanceFromCenterAffectingHeightBias;
        })).apply(instance, DripstoneClusterConfiguration::new);
    });
    public final int floorToCeilingSearchRange;
    public final IntProvider height;
    public final IntProvider radius;
    public final int maxStalagmiteStalactiteHeightDiff;
    public final int heightDeviation;
    public final IntProvider dripstoneBlockLayerThickness;
    public final FloatProvider density;
    public final FloatProvider wetness;
    public final float chanceOfDripstoneColumnAtMaxDistanceFromCenter;
    public final int maxDistanceFromEdgeAffectingChanceOfDripstoneColumn;
    public final int maxDistanceFromCenterAffectingHeightBias;

    public DripstoneClusterConfiguration(int floorToCeilingSearchRange, IntProvider height, IntProvider radius, int maxStalagmiteStalactiteHeightDiff, int heightDeviation, IntProvider dripstoneBlockLayerThickness, FloatProvider density, FloatProvider wetness, float wetnessMean, int i, int j) {
        this.floorToCeilingSearchRange = floorToCeilingSearchRange;
        this.height = height;
        this.radius = radius;
        this.maxStalagmiteStalactiteHeightDiff = maxStalagmiteStalactiteHeightDiff;
        this.heightDeviation = heightDeviation;
        this.dripstoneBlockLayerThickness = dripstoneBlockLayerThickness;
        this.density = density;
        this.wetness = wetness;
        this.chanceOfDripstoneColumnAtMaxDistanceFromCenter = wetnessMean;
        this.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn = i;
        this.maxDistanceFromCenterAffectingHeightBias = j;
    }
}
