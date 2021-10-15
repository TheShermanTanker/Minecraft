package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;

public class WorldGenDecoratorRange extends VerticalDecorator<WorldGenFeatureChanceDecoratorRangeConfiguration> {
    public WorldGenDecoratorRange(Codec<WorldGenFeatureChanceDecoratorRangeConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int y(WorldGenDecoratorContext context, Random random, WorldGenFeatureChanceDecoratorRangeConfiguration config, int y) {
        return config.height.sample(random, context);
    }
}
