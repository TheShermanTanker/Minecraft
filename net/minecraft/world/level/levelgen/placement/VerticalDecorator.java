package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public abstract class VerticalDecorator<DC extends WorldGenFeatureDecoratorConfiguration> extends WorldGenDecorator<DC> {
    public VerticalDecorator(Codec<DC> configCodec) {
        super(configCodec);
    }

    protected abstract int y(WorldGenDecoratorContext context, Random random, DC config, int y);

    @Override
    public final Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, DC config, BlockPosition pos) {
        return Stream.of(new BlockPosition(pos.getX(), this.y(context, random, config, pos.getY()), pos.getZ()));
    }
}
