package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;

public class InSquarePlacement extends PlacementModifier {
    private static final InSquarePlacement INSTANCE = new InSquarePlacement();
    public static final Codec<InSquarePlacement> CODEC = Codec.unit(() -> {
        return INSTANCE;
    });

    public static InSquarePlacement spread() {
        return INSTANCE;
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        int i = random.nextInt(16) + pos.getX();
        int j = random.nextInt(16) + pos.getZ();
        return Stream.of(new BlockPosition(i, pos.getY(), j));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.IN_SQUARE;
    }
}
