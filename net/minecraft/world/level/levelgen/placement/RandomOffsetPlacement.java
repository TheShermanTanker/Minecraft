package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;

public class RandomOffsetPlacement extends PlacementModifier {
    public static final Codec<RandomOffsetPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IntProvider.codec(-16, 16).fieldOf("xz_spread").forGetter((randomOffsetPlacement) -> {
            return randomOffsetPlacement.xzSpread;
        }), IntProvider.codec(-16, 16).fieldOf("y_spread").forGetter((randomOffsetPlacement) -> {
            return randomOffsetPlacement.ySpread;
        })).apply(instance, RandomOffsetPlacement::new);
    });
    private final IntProvider xzSpread;
    private final IntProvider ySpread;

    public static RandomOffsetPlacement of(IntProvider spreadXz, IntProvider spreadY) {
        return new RandomOffsetPlacement(spreadXz, spreadY);
    }

    public static RandomOffsetPlacement vertical(IntProvider spreadY) {
        return new RandomOffsetPlacement(IntProviderConstant.of(0), spreadY);
    }

    public static RandomOffsetPlacement horizontal(IntProvider spreadXz) {
        return new RandomOffsetPlacement(spreadXz, IntProviderConstant.of(0));
    }

    private RandomOffsetPlacement(IntProvider xzSpread, IntProvider ySpread) {
        this.xzSpread = xzSpread;
        this.ySpread = ySpread;
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        int i = pos.getX() + this.xzSpread.sample(random);
        int j = pos.getY() + this.ySpread.sample(random);
        int k = pos.getZ() + this.xzSpread.sample(random);
        return Stream.of(new BlockPosition(i, j, k));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.RANDOM_OFFSET;
    }
}
