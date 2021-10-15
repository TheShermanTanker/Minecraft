package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;

public class WorldGenDecoratorIceburg extends WorldGenDecorator<WorldGenFeatureEmptyConfiguration2> {
    public WorldGenDecoratorIceburg(Codec<WorldGenFeatureEmptyConfiguration2> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenFeatureEmptyConfiguration2 config, BlockPosition pos) {
        int i = random.nextInt(8) + 4 + pos.getX();
        int j = random.nextInt(8) + 4 + pos.getZ();
        return Stream.of(new BlockPosition(i, pos.getY(), j));
    }
}
