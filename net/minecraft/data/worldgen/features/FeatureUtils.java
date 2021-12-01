package net.minecraft.data.worldgen.features;

import java.util.List;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class FeatureUtils {
    public static WorldGenFeatureConfigured<?, ?> bootstrap() {
        WorldGenFeatureConfigured<?, ?>[] configuredFeatures = new WorldGenFeatureConfigured[]{AquaticFeatures.KELP, CaveFeatures.MOSS_PATCH_BONEMEAL, EndFeatures.CHORUS_PLANT, MiscOverworldFeatures.SPRING_LAVA_OVERWORLD, NetherFeatures.BASALT_BLOBS, OreFeatures.ORE_ANCIENT_DEBRIS_LARGE, PileFeatures.PILE_HAY, TreeFeatures.AZALEA_TREE, VegetationFeatures.TREES_OLD_GROWTH_PINE_TAIGA};
        return SystemUtils.getRandom(configuredFeatures, new Random());
    }

    private static BlockPredicate simplePatchPredicate(List<Block> validGround) {
        BlockPredicate blockPredicate;
        if (!validGround.isEmpty()) {
            blockPredicate = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(validGround, new BlockPosition(0, -1, 0)));
        } else {
            blockPredicate = BlockPredicate.ONLY_IN_AIR_PREDICATE;
        }

        return blockPredicate;
    }

    public static WorldGenFeatureRandomPatchConfiguration simpleRandomPatchConfiguration(int tries, PlacedFeature feature) {
        return new WorldGenFeatureRandomPatchConfiguration(tries, 7, 3, () -> {
            return feature;
        });
    }

    public static WorldGenFeatureRandomPatchConfiguration simplePatchConfiguration(WorldGenFeatureConfigured<?, ?> feature, List<Block> validGround, int tries) {
        return simpleRandomPatchConfiguration(tries, feature.filtered(simplePatchPredicate(validGround)));
    }

    public static WorldGenFeatureRandomPatchConfiguration simplePatchConfiguration(WorldGenFeatureConfigured<?, ?> feature, List<Block> validGround) {
        return simplePatchConfiguration(feature, validGround, 96);
    }

    public static WorldGenFeatureRandomPatchConfiguration simplePatchConfiguration(WorldGenFeatureConfigured<?, ?> feature) {
        return simplePatchConfiguration(feature, List.of(), 96);
    }

    public static <FC extends WorldGenFeatureConfiguration> WorldGenFeatureConfigured<FC, ?> register(String id, WorldGenFeatureConfigured<FC, ?> configuredFeature) {
        return IRegistry.register(RegistryGeneration.CONFIGURED_FEATURE, id, configuredFeature);
    }
}
