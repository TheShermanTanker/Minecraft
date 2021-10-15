package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenMegaTreeProviderDarkOak extends WorldGenMegaTreeProvider {
    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredFeature(Random random, boolean bees) {
        return null;
    }

    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredMegaFeature(Random random) {
        return BiomeDecoratorGroups.DARK_OAK;
    }
}
