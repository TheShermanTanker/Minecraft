package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorDungeonConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenDecoratorDungeonConfiguration> CODEC = Codec.INT.fieldOf("chance").xmap(WorldGenDecoratorDungeonConfiguration::new, (chanceDecoratorConfiguration) -> {
        return chanceDecoratorConfiguration.chance;
    }).codec();
    public final int chance;

    public WorldGenDecoratorDungeonConfiguration(int chance) {
        this.chance = chance;
    }
}
