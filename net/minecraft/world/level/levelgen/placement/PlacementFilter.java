package net.minecraft.world.level.levelgen.placement;

import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;

public abstract class PlacementFilter extends PlacementModifier {
    @Override
    public final Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        return this.shouldPlace(context, random, pos) ? Stream.of(pos) : Stream.of();
    }

    protected abstract boolean shouldPlace(PlacementContext context, Random random, BlockPosition pos);
}
