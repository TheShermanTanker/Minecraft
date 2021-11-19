package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.WorldGenBiomeDecoratorGroups;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenTreeProviderOak extends WorldGenTreeProvider {
    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredFeature(Random random, boolean bees) {
        if (random.nextInt(10) == 0) {
            return bees ? WorldGenBiomeDecoratorGroups.FANCY_OAK_BEES_005 : WorldGenBiomeDecoratorGroups.FANCY_OAK;
        } else {
            return bees ? WorldGenBiomeDecoratorGroups.OAK_BEES_005 : WorldGenBiomeDecoratorGroups.OAK;
        }
    }
}
