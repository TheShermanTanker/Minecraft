package net.minecraft.data.worldgen.features;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.IntProviderConstant;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockHugeMushroom;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureHugeFungiConfiguration;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureMushroomConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeThreeLayers;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeTwoLayers;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerAcacia;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerBlob;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerBush;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerDarkOak;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerFancy;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerJungle;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerMegaPine;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerPine;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacerSpruce;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoliagePlacerRandomSpread;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviderWeighted;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTreeAlterGround;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTreeBeehive;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTreeCocoa;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTreeVineLeaves;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTreeVineTrunk;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerBending;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerDarkOak;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerFancy;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerForking;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerGiant;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerMegaJungle;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerStraight;

public class TreeFeatures {
    public static final WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?> CRIMSON_FUNGUS = FeatureUtils.register("crimson_fungus", WorldGenerator.HUGE_FUNGUS.configured(new WorldGenFeatureHugeFungiConfiguration(Blocks.CRIMSON_NYLIUM.getBlockData(), Blocks.CRIMSON_STEM.getBlockData(), Blocks.NETHER_WART_BLOCK.getBlockData(), Blocks.SHROOMLIGHT.getBlockData(), false)));
    public static final WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?> CRIMSON_FUNGUS_PLANTED = FeatureUtils.register("crimson_fungus_planted", WorldGenerator.HUGE_FUNGUS.configured(new WorldGenFeatureHugeFungiConfiguration(Blocks.CRIMSON_NYLIUM.getBlockData(), Blocks.CRIMSON_STEM.getBlockData(), Blocks.NETHER_WART_BLOCK.getBlockData(), Blocks.SHROOMLIGHT.getBlockData(), true)));
    public static final WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?> WARPED_FUNGUS = FeatureUtils.register("warped_fungus", WorldGenerator.HUGE_FUNGUS.configured(new WorldGenFeatureHugeFungiConfiguration(Blocks.WARPED_NYLIUM.getBlockData(), Blocks.WARPED_STEM.getBlockData(), Blocks.WARPED_WART_BLOCK.getBlockData(), Blocks.SHROOMLIGHT.getBlockData(), false)));
    public static final WorldGenFeatureConfigured<WorldGenFeatureHugeFungiConfiguration, ?> WARPED_FUNGUS_PLANTED = FeatureUtils.register("warped_fungus_planted", WorldGenerator.HUGE_FUNGUS.configured(new WorldGenFeatureHugeFungiConfiguration(Blocks.WARPED_NYLIUM.getBlockData(), Blocks.WARPED_STEM.getBlockData(), Blocks.WARPED_WART_BLOCK.getBlockData(), Blocks.SHROOMLIGHT.getBlockData(), true)));
    public static final WorldGenFeatureConfigured<?, ?> HUGE_BROWN_MUSHROOM = FeatureUtils.register("huge_brown_mushroom", WorldGenerator.HUGE_BROWN_MUSHROOM.configured(new WorldGenFeatureMushroomConfiguration(WorldGenFeatureStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK.getBlockData().set(BlockHugeMushroom.UP, Boolean.valueOf(true)).set(BlockHugeMushroom.DOWN, Boolean.valueOf(false))), WorldGenFeatureStateProvider.simple(Blocks.MUSHROOM_STEM.getBlockData().set(BlockHugeMushroom.UP, Boolean.valueOf(false)).set(BlockHugeMushroom.DOWN, Boolean.valueOf(false))), 3)));
    public static final WorldGenFeatureConfigured<?, ?> HUGE_RED_MUSHROOM = FeatureUtils.register("huge_red_mushroom", WorldGenerator.HUGE_RED_MUSHROOM.configured(new WorldGenFeatureMushroomConfiguration(WorldGenFeatureStateProvider.simple(Blocks.RED_MUSHROOM_BLOCK.getBlockData().set(BlockHugeMushroom.DOWN, Boolean.valueOf(false))), WorldGenFeatureStateProvider.simple(Blocks.MUSHROOM_STEM.getBlockData().set(BlockHugeMushroom.UP, Boolean.valueOf(false)).set(BlockHugeMushroom.DOWN, Boolean.valueOf(false))), 2)));
    private static final WorldGenFeatureTreeBeehive BEEHIVE_0002 = new WorldGenFeatureTreeBeehive(0.002F);
    private static final WorldGenFeatureTreeBeehive BEEHIVE_002 = new WorldGenFeatureTreeBeehive(0.02F);
    private static final WorldGenFeatureTreeBeehive BEEHIVE_005 = new WorldGenFeatureTreeBeehive(0.05F);
    private static final WorldGenFeatureTreeBeehive BEEHIVE = new WorldGenFeatureTreeBeehive(1.0F);
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> OAK = FeatureUtils.register("oak", WorldGenerator.TREE.configured(createOak().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> DARK_OAK = FeatureUtils.register("dark_oak", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.DARK_OAK_LOG), new TrunkPlacerDarkOak(6, 2, 1), WorldGenFeatureStateProvider.simple(Blocks.DARK_OAK_LEAVES), new WorldGenFoilagePlacerDarkOak(IntProviderConstant.of(0), IntProviderConstant.of(0)), new FeatureSizeThreeLayers(1, 1, 0, 1, 2, OptionalInt.empty()))).ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> BIRCH = FeatureUtils.register("birch", WorldGenerator.TREE.configured(createBirch().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> ACACIA = FeatureUtils.register("acacia", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.ACACIA_LOG), new TrunkPlacerForking(5, 2, 2), WorldGenFeatureStateProvider.simple(Blocks.ACACIA_LEAVES), new WorldGenFoilagePlacerAcacia(IntProviderConstant.of(2), IntProviderConstant.of(0)), new FeatureSizeTwoLayers(1, 0, 2))).ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> SPRUCE = FeatureUtils.register("spruce", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LOG), new TrunkPlacerStraight(5, 2, 1), WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LEAVES), new WorldGenFoilagePlacerSpruce(IntProviderUniform.of(2, 3), IntProviderUniform.of(0, 2), IntProviderUniform.of(1, 2)), new FeatureSizeTwoLayers(2, 0, 2))).ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> PINE = FeatureUtils.register("pine", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LOG), new TrunkPlacerStraight(6, 4, 0), WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LEAVES), new WorldGenFoilagePlacerPine(IntProviderConstant.of(1), IntProviderConstant.of(1), IntProviderUniform.of(3, 4)), new FeatureSizeTwoLayers(2, 0, 2))).ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> JUNGLE_TREE = FeatureUtils.register("jungle_tree", WorldGenerator.TREE.configured(createJungleTree().decorators(ImmutableList.of(new WorldGenFeatureTreeCocoa(0.2F), WorldGenFeatureTreeVineTrunk.INSTANCE, WorldGenFeatureTreeVineLeaves.INSTANCE)).ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> FANCY_OAK = FeatureUtils.register("fancy_oak", WorldGenerator.TREE.configured(createFancyOak().build()));
    public static final WorldGenFeatureConfigured<?, ?> JUNGLE_TREE_NO_VINE = FeatureUtils.register("jungle_tree_no_vine", WorldGenerator.TREE.configured(createJungleTree().ignoreVines().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> MEGA_JUNGLE_TREE = FeatureUtils.register("mega_jungle_tree", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.JUNGLE_LOG), new TrunkPlacerMegaJungle(10, 2, 19), WorldGenFeatureStateProvider.simple(Blocks.JUNGLE_LEAVES), new WorldGenFoilagePlacerJungle(IntProviderConstant.of(2), IntProviderConstant.of(0), 2), new FeatureSizeTwoLayers(1, 1, 2))).decorators(ImmutableList.of(WorldGenFeatureTreeVineTrunk.INSTANCE, WorldGenFeatureTreeVineLeaves.INSTANCE)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> MEGA_SPRUCE = FeatureUtils.register("mega_spruce", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LOG), new TrunkPlacerGiant(13, 2, 14), WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LEAVES), new WorldGenFoilagePlacerMegaPine(IntProviderConstant.of(0), IntProviderConstant.of(0), IntProviderUniform.of(13, 17)), new FeatureSizeTwoLayers(1, 1, 2))).decorators(ImmutableList.of(new WorldGenFeatureTreeAlterGround(WorldGenFeatureStateProvider.simple(Blocks.PODZOL)))).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> MEGA_PINE = FeatureUtils.register("mega_pine", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LOG), new TrunkPlacerGiant(13, 2, 14), WorldGenFeatureStateProvider.simple(Blocks.SPRUCE_LEAVES), new WorldGenFoilagePlacerMegaPine(IntProviderConstant.of(0), IntProviderConstant.of(0), IntProviderUniform.of(3, 7)), new FeatureSizeTwoLayers(1, 1, 2))).decorators(ImmutableList.of(new WorldGenFeatureTreeAlterGround(WorldGenFeatureStateProvider.simple(Blocks.PODZOL)))).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> SUPER_BIRCH_BEES_0002 = FeatureUtils.register("super_birch_bees_0002", WorldGenerator.TREE.configured(createSuperBirch().decorators(ImmutableList.of(BEEHIVE_0002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> SUPER_BIRCH_BEES = FeatureUtils.register("super_birch_bees", WorldGenerator.TREE.configured(createSuperBirch().decorators(ImmutableList.of(BEEHIVE)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> SWAMP_OAK = FeatureUtils.register("swamp_oak", WorldGenerator.TREE.configured(createStraightBlobTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 5, 3, 0, 3).decorators(ImmutableList.of(WorldGenFeatureTreeVineLeaves.INSTANCE)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> JUNGLE_BUSH = FeatureUtils.register("jungle_bush", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.JUNGLE_LOG), new TrunkPlacerStraight(1, 0, 0), WorldGenFeatureStateProvider.simple(Blocks.OAK_LEAVES), new WorldGenFoilagePlacerBush(IntProviderConstant.of(2), IntProviderConstant.of(1), 2), new FeatureSizeTwoLayers(0, 0, 0))).build()));
    public static final WorldGenFeatureConfigured<?, ?> AZALEA_TREE = FeatureUtils.register("azalea_tree", WorldGenerator.TREE.configured((new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.OAK_LOG), new TrunkPlacerBending(4, 2, 0, 3, IntProviderUniform.of(1, 2)), new WorldGenFeatureStateProviderWeighted(SimpleWeightedRandomList.<IBlockData>builder().add(Blocks.AZALEA_LEAVES.getBlockData(), 3).add(Blocks.FLOWERING_AZALEA_LEAVES.getBlockData(), 1)), new WorldGenFoliagePlacerRandomSpread(IntProviderConstant.of(3), IntProviderConstant.of(0), IntProviderConstant.of(2), 50), new FeatureSizeTwoLayers(1, 0, 1))).dirt(WorldGenFeatureStateProvider.simple(Blocks.ROOTED_DIRT)).forceDirt().build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> OAK_BEES_0002 = FeatureUtils.register("oak_bees_0002", WorldGenerator.TREE.configured(createOak().decorators(List.of(BEEHIVE_0002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> OAK_BEES_002 = FeatureUtils.register("oak_bees_002", WorldGenerator.TREE.configured(createOak().decorators(List.of(BEEHIVE_002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> OAK_BEES_005 = FeatureUtils.register("oak_bees_005", WorldGenerator.TREE.configured(createOak().decorators(List.of(BEEHIVE_005)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> BIRCH_BEES_0002 = FeatureUtils.register("birch_bees_0002", WorldGenerator.TREE.configured(createBirch().decorators(List.of(BEEHIVE_0002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> BIRCH_BEES_002 = FeatureUtils.register("birch_bees_002", WorldGenerator.TREE.configured(createBirch().decorators(List.of(BEEHIVE_002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> BIRCH_BEES_005 = FeatureUtils.register("birch_bees_005", WorldGenerator.TREE.configured(createBirch().decorators(List.of(BEEHIVE_005)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> FANCY_OAK_BEES_0002 = FeatureUtils.register("fancy_oak_bees_0002", WorldGenerator.TREE.configured(createFancyOak().decorators(List.of(BEEHIVE_0002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> FANCY_OAK_BEES_002 = FeatureUtils.register("fancy_oak_bees_002", WorldGenerator.TREE.configured(createFancyOak().decorators(List.of(BEEHIVE_002)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> FANCY_OAK_BEES_005 = FeatureUtils.register("fancy_oak_bees_005", WorldGenerator.TREE.configured(createFancyOak().decorators(List.of(BEEHIVE_005)).build()));
    public static final WorldGenFeatureConfigured<WorldGenFeatureTreeConfiguration, ?> FANCY_OAK_BEES = FeatureUtils.register("fancy_oak_bees", WorldGenerator.TREE.configured(createFancyOak().decorators(List.of(BEEHIVE)).build()));

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createStraightBlobTree(Block log, Block leaves, int baseHeight, int firstRandomHeight, int secondRandomHeight, int radius) {
        return new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(log), new TrunkPlacerStraight(baseHeight, firstRandomHeight, secondRandomHeight), WorldGenFeatureStateProvider.simple(leaves), new WorldGenFoilagePlacerBlob(IntProviderConstant.of(radius), IntProviderConstant.of(0), 3), new FeatureSizeTwoLayers(1, 0, 1));
    }

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createOak() {
        return createStraightBlobTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, 0, 2).ignoreVines();
    }

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createBirch() {
        return createStraightBlobTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 0, 2).ignoreVines();
    }

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createSuperBirch() {
        return createStraightBlobTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 6, 2).ignoreVines();
    }

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createJungleTree() {
        return createStraightBlobTree(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, 4, 8, 0, 2);
    }

    private static WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder createFancyOak() {
        return (new WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder(WorldGenFeatureStateProvider.simple(Blocks.OAK_LOG), new TrunkPlacerFancy(3, 11, 0), WorldGenFeatureStateProvider.simple(Blocks.OAK_LEAVES), new WorldGenFoilagePlacerFancy(IntProviderConstant.of(2), IntProviderConstant.of(4), 4), new FeatureSizeTwoLayers(0, 0, 0, OptionalInt.of(4)))).ignoreVines();
    }
}
