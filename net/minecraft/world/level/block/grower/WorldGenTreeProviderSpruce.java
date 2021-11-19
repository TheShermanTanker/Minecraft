package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.WorldGenBiomeDecoratorGroups;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenTreeProviderSpruce extends WorldGenMegaTreeProvider {
    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredFeature(Random random, boolean bees) {
        return WorldGenBiomeDecoratorGroups.SPRUCE;
    }

    @Nullable
    @Override
    protected WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> getConfiguredMegaFeature(Random random) {
        return random.nextBoolean() ? WorldGenBiomeDecoratorGroups.MEGA_SPRUCE : WorldGenBiomeDecoratorGroups.MEGA_PINE;
    }
}
