package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;

class TrueBlockPredicate implements BlockPredicate {
    public static TrueBlockPredicate INSTANCE = new TrueBlockPredicate();
    public static final Codec<TrueBlockPredicate> CODEC = Codec.unit(() -> {
        return INSTANCE;
    });

    private TrueBlockPredicate() {
    }

    @Override
    public boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        return true;
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.TRUE;
    }
}
