package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;

public class WorldGenDecoratorDecorated extends WorldGenDecorator<WorldGenDecoratorDecpratedConfiguration> {
    public WorldGenDecoratorDecorated(Codec<WorldGenDecoratorDecpratedConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenDecoratorDecpratedConfiguration config, BlockPosition pos) {
        return config.outer().getPositions(context, random, pos).flatMap((blockPos) -> {
            return config.inner().getPositions(context, random, blockPos);
        });
    }
}
