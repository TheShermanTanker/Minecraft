package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;

public enum WorldgenRandom$Algorithm {
    LEGACY(LegacyRandomSource::new),
    XOROSHIRO(XoroshiroRandomSource::new);

    private final LongFunction<RandomSource> constructor;

    private WorldgenRandom$Algorithm(LongFunction<RandomSource> provider) {
        this.constructor = provider;
    }

    public RandomSource newInstance(long seed) {
        return this.constructor.apply(seed);
    }
}
