package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorCarveMaskConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenDecoratorCarveMaskConfiguration> CODEC = WorldGenStage.Features.CODEC.fieldOf("step").xmap(WorldGenDecoratorCarveMaskConfiguration::new, (config) -> {
        return config.step;
    }).codec();
    protected final WorldGenStage.Features step;

    public WorldGenDecoratorCarveMaskConfiguration(WorldGenStage.Features carver) {
        this.step = carver;
    }
}
