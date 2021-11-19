package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;

public class WorldGenDecoratorFrequencyConfiguration implements WorldGenFeatureDecoratorConfiguration, WorldGenFeatureConfiguration {
    public static final Codec<WorldGenDecoratorFrequencyConfiguration> CODEC = IntProvider.codec(0, 256).fieldOf("count").xmap(WorldGenDecoratorFrequencyConfiguration::new, WorldGenDecoratorFrequencyConfiguration::count).codec();
    private final IntProvider count;

    public WorldGenDecoratorFrequencyConfiguration(int count) {
        this.count = IntProviderConstant.of(count);
    }

    public WorldGenDecoratorFrequencyConfiguration(IntProvider distribution) {
        this.count = distribution;
    }

    public IntProvider count() {
        return this.count;
    }
}
