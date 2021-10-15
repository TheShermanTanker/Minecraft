package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.configurations.HeightmapConfiguration;

public class WorldGenDecoratorHeightmap extends WorldGenDecorator<HeightmapConfiguration> {
    public WorldGenDecoratorHeightmap(Codec<HeightmapConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, HeightmapConfiguration config, BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getZ();
        int k = context.getHeight(config.heightmap, i, j);
        return k > context.getMinBuildHeight() ? Stream.of(new BlockPosition(i, k, j)) : Stream.of();
    }
}