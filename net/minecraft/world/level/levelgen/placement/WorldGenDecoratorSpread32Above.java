package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;

public class WorldGenDecoratorSpread32Above extends VerticalDecorator<WorldGenFeatureEmptyConfiguration2> {
    public WorldGenDecoratorSpread32Above(Codec<WorldGenFeatureEmptyConfiguration2> configCodec) {
        super(configCodec);
    }

    @Override
    protected int y(WorldGenDecoratorContext context, Random random, WorldGenFeatureEmptyConfiguration2 config, int y) {
        return random.nextInt(Math.max(y, 0) + 32);
    }
}
