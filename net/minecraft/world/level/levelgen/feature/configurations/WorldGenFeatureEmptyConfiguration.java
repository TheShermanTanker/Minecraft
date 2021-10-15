package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;

public class WorldGenFeatureEmptyConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureEmptyConfiguration> CODEC;
    public static final WorldGenFeatureEmptyConfiguration INSTANCE = new WorldGenFeatureEmptyConfiguration();

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
