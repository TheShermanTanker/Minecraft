package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class GrowingPlantConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<GrowingPlantConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SimpleWeightedRandomList.wrappedCodec(IntProvider.CODEC).fieldOf("height_distribution").forGetter((growingPlantConfiguration) -> {
            return growingPlantConfiguration.heightDistribution;
        }), EnumDirection.CODEC.fieldOf("direction").forGetter((growingPlantConfiguration) -> {
            return growingPlantConfiguration.direction;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("body_provider").forGetter((growingPlantConfiguration) -> {
            return growingPlantConfiguration.bodyProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("head_provider").forGetter((growingPlantConfiguration) -> {
            return growingPlantConfiguration.headProvider;
        }), Codec.BOOL.fieldOf("allow_water").forGetter((growingPlantConfiguration) -> {
            return growingPlantConfiguration.allowWater;
        })).apply(instance, GrowingPlantConfiguration::new);
    });
    public final SimpleWeightedRandomList<IntProvider> heightDistribution;
    public final EnumDirection direction;
    public final WorldGenFeatureStateProvider bodyProvider;
    public final WorldGenFeatureStateProvider headProvider;
    public final boolean allowWater;

    public GrowingPlantConfiguration(SimpleWeightedRandomList<IntProvider> heightDistribution, EnumDirection direction, WorldGenFeatureStateProvider bodyProvider, WorldGenFeatureStateProvider headProvider, boolean allowWater) {
        this.heightDistribution = heightDistribution;
        this.direction = direction;
        this.bodyProvider = bodyProvider;
        this.headProvider = headProvider;
        this.allowWater = allowWater;
    }
}
