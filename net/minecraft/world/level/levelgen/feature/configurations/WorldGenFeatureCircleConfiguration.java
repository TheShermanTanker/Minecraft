package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.IBlockData;

public record WorldGenFeatureCircleConfiguration(IBlockData state, IntProvider radius, int halfHeight, List<IBlockData> targets) implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureCircleConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("state").forGetter(WorldGenFeatureCircleConfiguration::state), IntProvider.codec(0, 8).fieldOf("radius").forGetter(WorldGenFeatureCircleConfiguration::radius), Codec.intRange(0, 4).fieldOf("half_height").forGetter(WorldGenFeatureCircleConfiguration::halfHeight), IBlockData.CODEC.listOf().fieldOf("targets").forGetter(WorldGenFeatureCircleConfiguration::targets)).apply(instance, WorldGenFeatureCircleConfiguration::new);
    });

    public WorldGenFeatureCircleConfiguration(IBlockData blockState, IntProvider intProvider, int i, List<IBlockData> list) {
        this.state = blockState;
        this.radius = intProvider;
        this.halfHeight = i;
        this.targets = list;
    }

    public IBlockData state() {
        return this.state;
    }

    public IntProvider radius() {
        return this.radius;
    }

    public int halfHeight() {
        return this.halfHeight;
    }

    public List<IBlockData> targets() {
        return this.targets;
    }
}
