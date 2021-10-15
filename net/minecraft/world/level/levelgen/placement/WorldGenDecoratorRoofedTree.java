package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;

public class WorldGenDecoratorRoofedTree extends WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> {
    public WorldGenDecoratorRoofedTree(Codec<WorldGenFeatureEmptyConfiguration2> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenFeatureEmptyConfiguration2 config, BlockPosition pos) {
        return IntStream.range(0, 16).mapToObj((i) -> {
            int j = i / 4;
            int k = i % 4;
            int l = j * 4 + 1 + random.nextInt(3) + pos.getX();
            int m = k * 4 + 1 + random.nextInt(3) + pos.getZ();
            return new BlockPosition(l, pos.getY(), m);
        });
    }
}
