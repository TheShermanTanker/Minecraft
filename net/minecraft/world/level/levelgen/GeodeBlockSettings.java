package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class GeodeBlockSettings {
    public final WorldGenFeatureStateProvider fillingProvider;
    public final WorldGenFeatureStateProvider innerLayerProvider;
    public final WorldGenFeatureStateProvider alternateInnerLayerProvider;
    public final WorldGenFeatureStateProvider middleLayerProvider;
    public final WorldGenFeatureStateProvider outerLayerProvider;
    public final List<IBlockData> innerPlacements;
    public final MinecraftKey cannotReplace;
    public final MinecraftKey invalidBlocks;
    public static final Codec<GeodeBlockSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("filling_provider").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.fillingProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("inner_layer_provider").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.innerLayerProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("alternate_inner_layer_provider").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.alternateInnerLayerProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("middle_layer_provider").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.middleLayerProvider;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("outer_layer_provider").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.outerLayerProvider;
        }), ExtraCodecs.nonEmptyList(IBlockData.CODEC.listOf()).fieldOf("inner_placements").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.innerPlacements;
        }), MinecraftKey.CODEC.fieldOf("cannot_replace").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.cannotReplace;
        }), MinecraftKey.CODEC.fieldOf("invalid_blocks").forGetter((geodeBlockSettings) -> {
            return geodeBlockSettings.invalidBlocks;
        })).apply(instance, GeodeBlockSettings::new);
    });

    public GeodeBlockSettings(WorldGenFeatureStateProvider fillingProvider, WorldGenFeatureStateProvider innerLayerProvider, WorldGenFeatureStateProvider alternateInnerLayerProvider, WorldGenFeatureStateProvider middleLayerProvider, WorldGenFeatureStateProvider outerLayerProvider, List<IBlockData> innerBlocks, MinecraftKey cannotReplace, MinecraftKey invalidBlocks) {
        this.fillingProvider = fillingProvider;
        this.innerLayerProvider = innerLayerProvider;
        this.alternateInnerLayerProvider = alternateInnerLayerProvider;
        this.middleLayerProvider = middleLayerProvider;
        this.outerLayerProvider = outerLayerProvider;
        this.innerPlacements = innerBlocks;
        this.cannotReplace = cannotReplace;
        this.invalidBlocks = invalidBlocks;
    }
}
