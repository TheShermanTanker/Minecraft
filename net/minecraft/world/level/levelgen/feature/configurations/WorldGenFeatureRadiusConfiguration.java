package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureRadiusConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRadiusConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("target").forGetter((replaceSphereConfiguration) -> {
            return replaceSphereConfiguration.targetState;
        }), IBlockData.CODEC.fieldOf("state").forGetter((replaceSphereConfiguration) -> {
            return replaceSphereConfiguration.replaceState;
        }), IntProvider.codec(0, 12).fieldOf("radius").forGetter((replaceSphereConfiguration) -> {
            return replaceSphereConfiguration.radius;
        })).apply(instance, WorldGenFeatureRadiusConfiguration::new);
    });
    public final IBlockData targetState;
    public final IBlockData replaceState;
    private final IntProvider radius;

    public WorldGenFeatureRadiusConfiguration(IBlockData target, IBlockData state, IntProvider radius) {
        this.targetState = target;
        this.replaceState = state;
        this.radius = radius;
    }

    public IntProvider radius() {
        return this.radius;
    }
}
