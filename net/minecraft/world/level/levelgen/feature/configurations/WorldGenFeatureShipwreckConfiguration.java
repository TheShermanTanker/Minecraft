package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;

public class WorldGenFeatureShipwreckConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureShipwreckConfiguration> CODEC = Codec.BOOL.fieldOf("is_beached").orElse(false).xmap(WorldGenFeatureShipwreckConfiguration::new, (shipwreckConfiguration) -> {
        return shipwreckConfiguration.isBeached;
    }).codec();
    public final boolean isBeached;

    public WorldGenFeatureShipwreckConfiguration(boolean isBeached) {
        this.isBeached = isBeached;
    }
}
