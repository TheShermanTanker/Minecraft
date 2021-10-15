package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;

public class WorldGenDecoratorCarveMask extends WorldGenDecorator<WorldGenDecoratorCarveMaskConfiguration> {
    public WorldGenDecoratorCarveMask(Codec<WorldGenDecoratorCarveMaskConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WorldGenDecoratorCarveMaskConfiguration config, BlockPosition pos) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(pos);
        BitSet bitSet = context.getCarvingMask(chunkPos, config.step);
        return IntStream.range(0, bitSet.length()).filter(bitSet::get).mapToObj((i) -> {
            int j = i & 15;
            int k = i >> 4 & 15;
            int l = i >> 8;
            return new BlockPosition(chunkPos.getMinBlockX() + j, l, chunkPos.getMinBlockZ() + k);
        });
    }
}
