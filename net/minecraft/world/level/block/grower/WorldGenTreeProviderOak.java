package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenTreeProviderOak extends WorldGenTreeProvider {
    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredFeature(Random random, boolean bees) {
        if (random.nextInt(10) == 0) {
            return bees ? BiomeDecoratorGroups.FANCY_OAK_BEES_005 : BiomeDecoratorGroups.FANCY_OAK;
        } else {
            return bees ? BiomeDecoratorGroups.OAK_BEES_005 : BiomeDecoratorGroups.OAK;
        }
    }
}
