package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;

public class WorldGenFeatureEmptyConfiguration2 implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenFeatureEmptyConfiguration2> CODEC;
    public static final WorldGenFeatureEmptyConfiguration2 INSTANCE = new WorldGenFeatureEmptyConfiguration2();

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
