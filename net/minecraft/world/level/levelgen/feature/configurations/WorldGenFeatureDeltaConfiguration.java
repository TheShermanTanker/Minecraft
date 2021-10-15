package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureDeltaConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureDeltaConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("contents").forGetter((deltaFeatureConfiguration) -> {
            return deltaFeatureConfiguration.contents;
        }), IBlockData.CODEC.fieldOf("rim").forGetter((deltaFeatureConfiguration) -> {
            return deltaFeatureConfiguration.rim;
        }), IntProvider.codec(0, 16).fieldOf("size").forGetter((deltaFeatureConfiguration) -> {
            return deltaFeatureConfiguration.size;
        }), IntProvider.codec(0, 16).fieldOf("rim_size").forGetter((deltaFeatureConfiguration) -> {
            return deltaFeatureConfiguration.rimSize;
        })).apply(instance, WorldGenFeatureDeltaConfiguration::new);
    });
    private final IBlockData contents;
    private final IBlockData rim;
    private final IntProvider size;
    private final IntProvider rimSize;

    public WorldGenFeatureDeltaConfiguration(IBlockData contents, IBlockData rim, IntProvider size, IntProvider rimSize) {
        this.contents = contents;
        this.rim = rim;
        this.size = size;
        this.rimSize = rimSize;
    }

    public IBlockData contents() {
        return this.contents;
    }

    public IBlockData rim() {
        return this.rim;
    }

    public IntProvider size() {
        return this.size;
    }

    public IntProvider rimSize() {
        return this.rimSize;
    }
}
