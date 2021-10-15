package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;

public class WorldGenDecoratorSquare extends WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> {
    public WorldGenDecoratorSquare(Codec<WorldGenFeatureEmptyConfiguration2> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenFeatureEmptyConfiguration2 config, BlockPosition pos) {
        int i = random.nextInt(16) + pos.getX();
        int j = random.nextInt(16) + pos.getZ();
        return Stream.of(new BlockPosition(i, pos.getY(), j));
    }
}
