package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviderSimpl;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTree;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

public class WorldGenFeatureTreeConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureTreeConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("trunk_provider").forGetter((treeConfiguration) -> {
            return treeConfiguration.trunkProvider;
        }), TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter((treeConfiguration) -> {
            return treeConfiguration.trunkPlacer;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("foliage_provider").forGetter((treeConfiguration) -> {
            return treeConfiguration.foliageProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("sapling_provider").forGetter((treeConfiguration) -> {
            return treeConfiguration.saplingProvider;
        }), WorldGenFoilagePlacer.CODEC.fieldOf("foliage_placer").forGetter((treeConfiguration) -> {
            return treeConfiguration.foliagePlacer;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("dirt_provider").forGetter((treeConfiguration) -> {
            return treeConfiguration.dirtProvider;
        }), FeatureSize.CODEC.fieldOf("minimum_size").forGetter((treeConfiguration) -> {
            return treeConfiguration.minimumSize;
        }), WorldGenFeatureTree.CODEC.listOf().fieldOf("decorators").forGetter((treeConfiguration) -> {
            return treeConfiguration.decorators;
        }), Codec.BOOL.fieldOf("ignore_vines").orElse(false).forGetter((treeConfiguration) -> {
            return treeConfiguration.ignoreVines;
        }), Codec.BOOL.fieldOf("force_dirt").orElse(false).forGetter((treeConfiguration) -> {
            return treeConfiguration.forceDirt;
        })).apply(instance, WorldGenFeatureTreeConfiguration::new);
    });
    public final WorldGenFeatureStateProvider trunkProvider;
    public final WorldGenFeatureStateProvider dirtProvider;
    public final TrunkPlacer trunkPlacer;
    public final WorldGenFeatureStateProvider foliageProvider;
    public final WorldGenFeatureStateProvider saplingProvider;
    public final WorldGenFoilagePlacer foliagePlacer;
    public final FeatureSize minimumSize;
    public final List<WorldGenFeatureTree> decorators;
    public final boolean ignoreVines;
    public final boolean forceDirt;

    protected WorldGenFeatureTreeConfiguration(WorldGenFeatureStateProvider trunkProvider, TrunkPlacer trunkPlacer, WorldGenFeatureStateProvider foliageProvider, WorldGenFeatureStateProvider saplingProvider, WorldGenFoilagePlacer foliagePlacer, WorldGenFeatureStateProvider dirtProvider, FeatureSize minimumSize, List<WorldGenFeatureTree> decorators, boolean ignoreVines, boolean forceDirt) {
        this.trunkProvider = trunkProvider;
        this.trunkPlacer = trunkPlacer;
        this.foliageProvider = foliageProvider;
        this.foliagePlacer = foliagePlacer;
        this.dirtProvider = dirtProvider;
        this.saplingProvider = saplingProvider;
        this.minimumSize = minimumSize;
        this.decorators = decorators;
        this.ignoreVines = ignoreVines;
        this.forceDirt = forceDirt;
    }

    public WorldGenFeatureTreeConfiguration withDecorators(List<WorldGenFeatureTree> decorators) {
        return new WorldGenFeatureTreeConfiguration(this.trunkProvider, this.trunkPlacer, this.foliageProvider, this.saplingProvider, this.foliagePlacer, this.dirtProvider, this.minimumSize, decorators, this.ignoreVines, this.forceDirt);
    }

    public static class TreeConfigurationBuilder {
        public final WorldGenFeatureStateProvider trunkProvider;
        private final TrunkPlacer trunkPlacer;
        public final WorldGenFeatureStateProvider foliageProvider;
        public final WorldGenFeatureStateProvider saplingProvider;
        private final WorldGenFoilagePlacer foliagePlacer;
        private WorldGenFeatureStateProvider dirtProvider;
        private final FeatureSize minimumSize;
        private List<WorldGenFeatureTree> decorators = ImmutableList.of();
        private boolean ignoreVines;
        private boolean forceDirt;

        public TreeConfigurationBuilder(WorldGenFeatureStateProvider trunkProvider, TrunkPlacer trunkPlacer, WorldGenFeatureStateProvider foliageProvider, WorldGenFeatureStateProvider saplingProvider, WorldGenFoilagePlacer foliagePlacer, FeatureSize minimumSize) {
            this.trunkProvider = trunkProvider;
            this.trunkPlacer = trunkPlacer;
            this.foliageProvider = foliageProvider;
            this.saplingProvider = saplingProvider;
            this.dirtProvider = new WorldGenFeatureStateProviderSimpl(Blocks.DIRT.getBlockData());
            this.foliagePlacer = foliagePlacer;
            this.minimumSize = minimumSize;
        }

        public WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder dirt(WorldGenFeatureStateProvider dirtProvider) {
            this.dirtProvider = dirtProvider;
            return this;
        }

        public WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder decorators(List<WorldGenFeatureTree> decorators) {
            this.decorators = decorators;
            return this;
        }

        public WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder ignoreVines() {
            this.ignoreVines = true;
            return this;
        }

        public WorldGenFeatureTreeConfiguration.TreeConfigurationBuilder forceDirt() {
            this.forceDirt = true;
            return this;
        }

        public WorldGenFeatureTreeConfiguration build() {
            return new WorldGenFeatureTreeConfiguration(this.trunkProvider, this.trunkPlacer, this.foliageProvider, this.saplingProvider, this.foliagePlacer, this.dirtProvider, this.minimumSize, this.decorators, this.ignoreVines, this.forceDirt);
        }
    }
}
