package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class VegetationPatchConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<VegetationPatchConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(MinecraftKey.CODEC.fieldOf("replaceable").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.replaceable;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("ground_state").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.groundState;
        }), PlacedFeature.CODEC.fieldOf("vegetation_feature").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.vegetationFeature;
        }), CaveSurface.CODEC.fieldOf("surface").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.surface;
        }), IntProvider.codec(1, 128).fieldOf("depth").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.depth;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("extra_bottom_block_chance").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.extraBottomBlockChance;
        }), Codec.intRange(1, 256).fieldOf("vertical_range").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.verticalRange;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("vegetation_chance").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.vegetationChance;
        }), IntProvider.CODEC.fieldOf("xz_radius").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.xzRadius;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("extra_edge_column_chance").forGetter((vegetationPatchConfiguration) -> {
            return vegetationPatchConfiguration.extraEdgeColumnChance;
        })).apply(instance, VegetationPatchConfiguration::new);
    });
    public final MinecraftKey replaceable;
    public final WorldGenFeatureStateProvider groundState;
    public final Supplier<PlacedFeature> vegetationFeature;
    public final CaveSurface surface;
    public final IntProvider depth;
    public final float extraBottomBlockChance;
    public final int verticalRange;
    public final float vegetationChance;
    public final IntProvider xzRadius;
    public final float extraEdgeColumnChance;

    public VegetationPatchConfiguration(MinecraftKey replaceable, WorldGenFeatureStateProvider groundState, Supplier<PlacedFeature> vegetationFeature, CaveSurface surface, IntProvider depth, float extraBottomBlockChance, int verticalRange, float vegetationChance, IntProvider horizontalRadius, float extraEdgeColumnChance) {
        this.replaceable = replaceable;
        this.groundState = groundState;
        this.vegetationFeature = vegetationFeature;
        this.surface = surface;
        this.depth = depth;
        this.extraBottomBlockChance = extraBottomBlockChance;
        this.verticalRange = verticalRange;
        this.vegetationChance = vegetationChance;
        this.xzRadius = horizontalRadius;
        this.extraEdgeColumnChance = extraEdgeColumnChance;
    }
}
