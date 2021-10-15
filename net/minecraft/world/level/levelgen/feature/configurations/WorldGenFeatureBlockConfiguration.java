package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class WorldGenFeatureBlockConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureBlockConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("to_place").forGetter((simpleBlockConfiguration) -> {
            return simpleBlockConfiguration.toPlace;
        }), IBlockData.CODEC.listOf().fieldOf("place_on").orElse(ImmutableList.of()).forGetter((simpleBlockConfiguration) -> {
            return simpleBlockConfiguration.placeOn;
        }), IBlockData.CODEC.listOf().fieldOf("place_in").orElse(ImmutableList.of()).forGetter((simpleBlockConfiguration) -> {
            return simpleBlockConfiguration.placeIn;
        }), IBlockData.CODEC.listOf().fieldOf("place_under").orElse(ImmutableList.of()).forGetter((simpleBlockConfiguration) -> {
            return simpleBlockConfiguration.placeUnder;
        })).apply(instance, WorldGenFeatureBlockConfiguration::new);
    });
    public final WorldGenFeatureStateProvider toPlace;
    public final List<IBlockData> placeOn;
    public final List<IBlockData> placeIn;
    public final List<IBlockData> placeUnder;

    public WorldGenFeatureBlockConfiguration(WorldGenFeatureStateProvider blockStateProvider, List<IBlockData> placeOn, List<IBlockData> placeIn, List<IBlockData> placeUnder) {
        this.toPlace = blockStateProvider;
        this.placeOn = placeOn;
        this.placeIn = placeIn;
        this.placeUnder = placeUnder;
    }

    public WorldGenFeatureBlockConfiguration(WorldGenFeatureStateProvider blockStateProvider) {
        this(blockStateProvider, ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
    }
}
