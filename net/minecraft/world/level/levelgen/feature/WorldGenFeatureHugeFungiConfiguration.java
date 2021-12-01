package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;

public class WorldGenFeatureHugeFungiConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureHugeFungiConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("valid_base_block").forGetter((hugeFungusConfiguration) -> {
            return hugeFungusConfiguration.validBaseState;
        }), IBlockData.CODEC.fieldOf("stem_state").forGetter((hugeFungusConfiguration) -> {
            return hugeFungusConfiguration.stemState;
        }), IBlockData.CODEC.fieldOf("hat_state").forGetter((hugeFungusConfiguration) -> {
            return hugeFungusConfiguration.hatState;
        }), IBlockData.CODEC.fieldOf("decor_state").forGetter((hugeFungusConfiguration) -> {
            return hugeFungusConfiguration.decorState;
        }), Codec.BOOL.fieldOf("planted").orElse(false).forGetter((hugeFungusConfiguration) -> {
            return hugeFungusConfiguration.planted;
        })).apply(instance, WorldGenFeatureHugeFungiConfiguration::new);
    });
    public final IBlockData validBaseState;
    public final IBlockData stemState;
    public final IBlockData hatState;
    public final IBlockData decorState;
    public final boolean planted;

    public WorldGenFeatureHugeFungiConfiguration(IBlockData validBaseBlock, IBlockData stemState, IBlockData hatState, IBlockData decorationState, boolean planted) {
        this.validBaseState = validBaseBlock;
        this.stemState = stemState;
        this.hatState = hatState;
        this.decorState = decorationState;
        this.planted = planted;
    }
}
