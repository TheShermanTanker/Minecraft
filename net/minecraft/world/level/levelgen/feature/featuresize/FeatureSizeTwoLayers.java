package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;

public class FeatureSizeTwoLayers extends FeatureSize {
    public static final Codec<FeatureSizeTwoLayers> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(0, 81).fieldOf("limit").orElse(1).forGetter((twoLayersFeatureSize) -> {
            return twoLayersFeatureSize.limit;
        }), Codec.intRange(0, 16).fieldOf("lower_size").orElse(0).forGetter((twoLayersFeatureSize) -> {
            return twoLayersFeatureSize.lowerSize;
        }), Codec.intRange(0, 16).fieldOf("upper_size").orElse(1).forGetter((twoLayersFeatureSize) -> {
            return twoLayersFeatureSize.upperSize;
        }), minClippedHeightCodec()).apply(instance, FeatureSizeTwoLayers::new);
    });
    private final int limit;
    private final int lowerSize;
    private final int upperSize;

    public FeatureSizeTwoLayers(int limit, int lowerSize, int upperSize) {
        this(limit, lowerSize, upperSize, OptionalInt.empty());
    }

    public FeatureSizeTwoLayers(int limit, int lowerSize, int upperSize, OptionalInt minClippedHeight) {
        super(minClippedHeight);
        this.limit = limit;
        this.lowerSize = lowerSize;
        this.upperSize = upperSize;
    }

    @Override
    protected FeatureSizeType<?> type() {
        return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
    }

    @Override
    public int getSizeAtHeight(int height, int y) {
        return y < this.limit ? this.lowerSize : this.upperSize;
    }
}
