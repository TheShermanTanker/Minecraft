package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;

public class WorldGenDecoratorChance extends RepeatingDecorator<WorldGenDecoratorDungeonConfiguration> {
    public WorldGenDecoratorChance(Codec<WorldGenDecoratorDungeonConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenDecoratorDungeonConfiguration config, BlockPosition pos) {
        return random.nextFloat() < 1.0F / (float)config.chance ? 1 : 0;
    }
}
