package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureCircleConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureCircleConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("state").forGetter((diskConfiguration) -> {
            return diskConfiguration.state;
        }), IntProvider.codec(0, 8).fieldOf("radius").forGetter((diskConfiguration) -> {
            return diskConfiguration.radius;
        }), Codec.intRange(0, 4).fieldOf("half_height").forGetter((diskConfiguration) -> {
            return diskConfiguration.halfHeight;
        }), IBlockData.CODEC.listOf().fieldOf("targets").forGetter((diskConfiguration) -> {
            return diskConfiguration.targets;
        })).apply(instance, WorldGenFeatureCircleConfiguration::new);
    });
    public final IBlockData state;
    public final IntProvider radius;
    public final int halfHeight;
    public final List<IBlockData> targets;

    public WorldGenFeatureCircleConfiguration(IBlockData state, IntProvider radius, int halfHeight, List<IBlockData> targets) {
        this.state = state;
        this.radius = radius;
        this.halfHeight = halfHeight;
        this.targets = targets;
    }
}
