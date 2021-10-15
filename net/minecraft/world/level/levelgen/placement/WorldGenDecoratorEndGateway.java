package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;

public class WorldGenDecoratorEndGateway extends VerticalDecorator<WorldGenFeatureEmptyConfiguration2> {
    public WorldGenDecoratorEndGateway(Codec<WorldGenFeatureEmptyConfiguration2> configCodec) {
        super(configCodec);
    }

    @Override
    protected int y(WorldGenDecoratorContext context, Random random, WorldGenFeatureEmptyConfiguration2 config, int y) {
        return y + 3 + random.nextInt(7);
    }
}
