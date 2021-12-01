package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;

public class CountPlacement extends RepeatingPlacement {
    public static final Codec<CountPlacement> CODEC = IntProvider.codec(0, 256).fieldOf("count").xmap(CountPlacement::new, (countPlacement) -> {
        return countPlacement.count;
    }).codec();
    private final IntProvider count;

    private CountPlacement(IntProvider count) {
        this.count = count;
    }

    public static CountPlacement of(IntProvider count) {
        return new CountPlacement(count);
    }

    public static CountPlacement of(int count) {
        return of(IntProviderConstant.of(count));
    }

    @Override
    protected int count(Random random, BlockPosition pos) {
        return this.count.sample(random);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.COUNT;
    }
}
