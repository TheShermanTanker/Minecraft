package net.minecraft.world.level.block.grower;

import java.util.Random;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public class WorldGenTreeProviderOak extends WorldGenTreeProvider {
    @Override
    protected WorldGenFeatureConfigured<?, ?> getConfiguredFeature(Random random, boolean bees) {
        if (random.nextInt(10) == 0) {
            return bees ? TreeFeatures.FANCY_OAK_BEES_005 : TreeFeatures.FANCY_OAK;
        } else {
            return bees ? TreeFeatures.OAK_BEES_005 : TreeFeatures.OAK;
        }
    }
}
