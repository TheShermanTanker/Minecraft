package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public class WorldGenMegaTreeProviderDarkOak extends WorldGenMegaTreeProvider {
    @Nullable
    @Override
    protected WorldGenFeatureConfigured<?, ?> getConfiguredFeature(Random random, boolean bees) {
        return null;
    }

    @Nullable
    @Override
    protected WorldGenFeatureConfigured<?, ?> getConfiguredMegaFeature(Random random) {
        return TreeFeatures.DARK_OAK;
    }
}
