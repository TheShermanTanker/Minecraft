package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorFrequencyExtraChanceConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenDecoratorFrequencyExtraChanceConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("count").forGetter((frequencyWithExtraChanceDecoratorConfiguration) -> {
            return frequencyWithExtraChanceDecoratorConfiguration.count;
        }), Codec.FLOAT.fieldOf("extra_chance").forGetter((frequencyWithExtraChanceDecoratorConfiguration) -> {
            return frequencyWithExtraChanceDecoratorConfiguration.extraChance;
        }), Codec.INT.fieldOf("extra_count").forGetter((frequencyWithExtraChanceDecoratorConfiguration) -> {
            return frequencyWithExtraChanceDecoratorConfiguration.extraCount;
        })).apply(instance, WorldGenDecoratorFrequencyExtraChanceConfiguration::new);
    });
    public final int count;
    public final float extraChance;
    public final int extraCount;

    public WorldGenDecoratorFrequencyExtraChanceConfiguration(int count, float extraChance, int extraCount) {
        this.count = count;
        this.extraChance = extraChance;
        this.extraCount = extraCount;
    }
}
