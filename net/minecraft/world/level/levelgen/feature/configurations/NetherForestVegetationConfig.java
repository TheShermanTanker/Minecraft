package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class NetherForestVegetationConfig extends WorldGenFeatureBlockPileConfiguration {
    public static final Codec<NetherForestVegetationConfig> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("state_provider").forGetter((netherForestVegetationConfig) -> {
            return netherForestVegetationConfig.stateProvider;
        }), ExtraCodecs.POSITIVE_INT.fieldOf("spread_width").forGetter((netherForestVegetationConfig) -> {
            return netherForestVegetationConfig.spreadWidth;
        }), ExtraCodecs.POSITIVE_INT.fieldOf("spread_height").forGetter((netherForestVegetationConfig) -> {
            return netherForestVegetationConfig.spreadHeight;
        })).apply(instance, NetherForestVegetationConfig::new);
    });
    public final int spreadWidth;
    public final int spreadHeight;

    public NetherForestVegetationConfig(WorldGenFeatureStateProvider stateProvider, int spreadWidth, int spreadHeight) {
        super(stateProvider);
        this.spreadWidth = spreadWidth;
        this.spreadHeight = spreadHeight;
    }
}
