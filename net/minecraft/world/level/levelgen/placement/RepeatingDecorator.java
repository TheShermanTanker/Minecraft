package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public abstract class RepeatingDecorator<DC extends WorldGenFeatureDecoratorConfiguration> extends WorldGenDecorator<DC> {
    public RepeatingDecorator(Codec<DC> configCodec) {
        super(configCodec);
    }

    protected abstract int count(Random random, DC config, BlockPosition pos);

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, DC config, BlockPosition pos) {
        return IntStream.range(0, this.count(random, config, pos)).mapToObj((i) -> {
            return pos;
        });
    }
}
