package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.blockplacers.WorldGenBlockPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class WorldGenFeatureRandomPatchConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRandomPatchConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("state_provider").forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.stateProvider;
        }), WorldGenBlockPlacer.CODEC.fieldOf("block_placer").forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.blockPlacer;
        }), IBlockData.CODEC.listOf().fieldOf("whitelist").forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.whitelist.stream().map(Block::getBlockData).collect(Collectors.toList());
        }), IBlockData.CODEC.listOf().fieldOf("blacklist").forGetter((randomPatchConfiguration) -> {
            return ImmutableList.copyOf(randomPatchConfiguration.blacklist);
        }), ExtraCodecs.POSITIVE_INT.fieldOf("tries").orElse(128).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.tries;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("xspread").orElse(7).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.xspread;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("yspread").orElse(3).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.yspread;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("zspread").orElse(7).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.zspread;
        }), Codec.BOOL.fieldOf("can_replace").orElse(false).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.canReplace;
        }), Codec.BOOL.fieldOf("project").orElse(true).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.project;
        }), Codec.BOOL.fieldOf("need_water").orElse(false).forGetter((randomPatchConfiguration) -> {
            return randomPatchConfiguration.needWater;
        })).apply(instance, WorldGenFeatureRandomPatchConfiguration::new);
    });
    public final WorldGenFeatureStateProvider stateProvider;
    public final WorldGenBlockPlacer blockPlacer;
    public final Set<Block> whitelist;
    public final Set<IBlockData> blacklist;
    public final int tries;
    public final int xspread;
    public final int yspread;
    public final int zspread;
    public final boolean canReplace;
    public final boolean project;
    public final boolean needWater;

    private WorldGenFeatureRandomPatchConfiguration(WorldGenFeatureStateProvider stateProvider, WorldGenBlockPlacer blockPlacer, List<IBlockData> whitelist, List<IBlockData> blacklist, int tries, int spreadX, int spreadY, int spreadZ, boolean canReplace, boolean project, boolean needsWater) {
        this(stateProvider, blockPlacer, whitelist.stream().map(BlockBase.BlockData::getBlock).collect(Collectors.toSet()), ImmutableSet.copyOf(blacklist), tries, spreadX, spreadY, spreadZ, canReplace, project, needsWater);
    }

    WorldGenFeatureRandomPatchConfiguration(WorldGenFeatureStateProvider stateProvider, WorldGenBlockPlacer blockPlacer, Set<Block> whitelist, Set<IBlockData> blacklist, int tries, int spreadX, int spreadY, int spreadZ, boolean canReplace, boolean project, boolean needsWater) {
        this.stateProvider = stateProvider;
        this.blockPlacer = blockPlacer;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
        this.tries = tries;
        this.xspread = spreadX;
        this.yspread = spreadY;
        this.zspread = spreadZ;
        this.canReplace = canReplace;
        this.project = project;
        this.needWater = needsWater;
    }

    public static class GrassConfigurationBuilder {
        private final WorldGenFeatureStateProvider stateProvider;
        private final WorldGenBlockPlacer blockPlacer;
        private Set<Block> whitelist = ImmutableSet.of();
        private Set<IBlockData> blacklist = ImmutableSet.of();
        private int tries = 64;
        private int xspread = 7;
        private int yspread = 3;
        private int zspread = 7;
        private boolean canReplace;
        private boolean project = true;
        private boolean needWater;

        public GrassConfigurationBuilder(WorldGenFeatureStateProvider stateProvider, WorldGenBlockPlacer blockPlacer) {
            this.stateProvider = stateProvider;
            this.blockPlacer = blockPlacer;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder whitelist(Set<Block> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder blacklist(Set<IBlockData> blacklist) {
            this.blacklist = blacklist;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder tries(int tries) {
            this.tries = tries;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder xspread(int spreadX) {
            this.xspread = spreadX;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder yspread(int spreadY) {
            this.yspread = spreadY;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder zspread(int spreadZ) {
            this.zspread = spreadZ;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder canReplace() {
            this.canReplace = true;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder noProjection() {
            this.project = false;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration.GrassConfigurationBuilder needWater() {
            this.needWater = true;
            return this;
        }

        public WorldGenFeatureRandomPatchConfiguration build() {
            return new WorldGenFeatureRandomPatchConfiguration(this.stateProvider, this.blockPlacer, this.whitelist, this.blacklist, this.tries, this.xspread, this.yspread, this.zspread, this.canReplace, this.project, this.needWater);
        }
    }
}
