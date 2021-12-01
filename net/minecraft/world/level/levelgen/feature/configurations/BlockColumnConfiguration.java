package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public record BlockColumnConfiguration(List<BlockColumnConfiguration.Layer> layers, EnumDirection direction, BlockPredicate allowedPlacement, boolean prioritizeTip) implements WorldGenFeatureConfiguration {
    public static final Codec<BlockColumnConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockColumnConfiguration.Layer.CODEC.listOf().fieldOf("layers").forGetter(BlockColumnConfiguration::layers), EnumDirection.CODEC.fieldOf("direction").forGetter(BlockColumnConfiguration::direction), BlockPredicate.CODEC.fieldOf("allowed_placement").forGetter(BlockColumnConfiguration::allowedPlacement), Codec.BOOL.fieldOf("prioritize_tip").forGetter(BlockColumnConfiguration::prioritizeTip)).apply(instance, BlockColumnConfiguration::new);
    });

    public BlockColumnConfiguration(List<BlockColumnConfiguration.Layer> list, EnumDirection direction, BlockPredicate blockPredicate, boolean bl) {
        this.layers = list;
        this.direction = direction;
        this.allowedPlacement = blockPredicate;
        this.prioritizeTip = bl;
    }

    public static BlockColumnConfiguration.Layer layer(IntProvider height, WorldGenFeatureStateProvider state) {
        return new BlockColumnConfiguration.Layer(height, state);
    }

    public static BlockColumnConfiguration simple(IntProvider height, WorldGenFeatureStateProvider state) {
        return new BlockColumnConfiguration(List.of(layer(height, state)), EnumDirection.UP, BlockPredicate.matchesBlock(Blocks.AIR, BlockPosition.ZERO), false);
    }

    public List<BlockColumnConfiguration.Layer> layers() {
        return this.layers;
    }

    public EnumDirection direction() {
        return this.direction;
    }

    public BlockPredicate allowedPlacement() {
        return this.allowedPlacement;
    }

    public boolean prioritizeTip() {
        return this.prioritizeTip;
    }

    public static record Layer(IntProvider height, WorldGenFeatureStateProvider state) {
        public static final Codec<BlockColumnConfiguration.Layer> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(IntProvider.NON_NEGATIVE_CODEC.fieldOf("height").forGetter(BlockColumnConfiguration.Layer::height), WorldGenFeatureStateProvider.CODEC.fieldOf("provider").forGetter(BlockColumnConfiguration.Layer::state)).apply(instance, BlockColumnConfiguration.Layer::new);
        });

        public Layer(IntProvider intProvider, WorldGenFeatureStateProvider blockStateProvider) {
            this.height = intProvider;
            this.state = blockStateProvider;
        }

        public IntProvider height() {
            return this.height;
        }

        public WorldGenFeatureStateProvider state() {
            return this.state;
        }
    }
}
