package net.minecraft.world.level.block.grower;

import java.util.Random;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public class WorldGenTreeProviderBirch extends WorldGenTreeProvider {
    @Override
    protected WorldGenFeatureConfigured<?, ?> getConfiguredFeature(Random random, boolean bees) {
        return bees ? TreeFeatures.BIRCH_BEES_005 : TreeFeatures.BIRCH;
    }
}
