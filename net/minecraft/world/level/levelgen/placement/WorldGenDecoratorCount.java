package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenDecoratorFrequencyConfiguration;

public class WorldGenDecoratorCount extends RepeatingDecorator<WorldGenDecoratorFrequencyConfiguration> {
    public WorldGenDecoratorCount(Codec<WorldGenDecoratorFrequencyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenDecoratorFrequencyConfiguration config, BlockPosition pos) {
        return config.count().sample(random);
    }
}
