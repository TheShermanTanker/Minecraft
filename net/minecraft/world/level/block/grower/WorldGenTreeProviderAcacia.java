package net.minecraft.world.level.block.grower;

import java.util.Random;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public class WorldGenTreeProviderAcacia extends WorldGenTreeProvider {
    @Override
    protected WorldGenFeatureConfigured<?, ?> getConfiguredFeature(Random random, boolean bees) {
        return TreeFeatures.ACACIA;
    }
}
