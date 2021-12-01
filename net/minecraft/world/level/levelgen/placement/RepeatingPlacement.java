package net.minecraft.world.level.levelgen.placement;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;

public abstract class RepeatingPlacement extends PlacementModifier {
    protected abstract int count(Random random, BlockPosition pos);

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        return IntStream.range(0, this.count(random, pos)).mapToObj((i) -> {
            return pos;
        });
    }
}
