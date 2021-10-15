package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;

public class WorldGenDecoratorCountExtra extends RepeatingDecorator<WorldGenDecoratorFrequencyExtraChanceConfiguration> {
    public WorldGenDecoratorCountExtra(Codec<WorldGenDecoratorFrequencyExtraChanceConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenDecoratorFrequencyExtraChanceConfiguration config, BlockPosition pos) {
        return config.count + (random.nextFloat() < config.extraChance ? config.extraCount : 0);
    }
}
