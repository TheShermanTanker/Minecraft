package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureEmpty extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureEmpty(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        return true;
    }
}
