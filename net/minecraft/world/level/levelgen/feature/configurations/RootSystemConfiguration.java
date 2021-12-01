package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RootSystemConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(PlacedFeature.CODEC.fieldOf("feature").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.treeFeature;
        }), Codec.intRange(1, 64).fieldOf("required_vertical_space_for_tree").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.requiredVerticalSpaceForTree;
        }), Codec.intRange(1, 64).fieldOf("root_radius").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.rootRadius;
        }), MinecraftKey.CODEC.fieldOf("root_replaceable").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.rootReplaceable;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("root_state_provider").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.rootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.rootPlacementAttempts;
        }), Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.rootColumnMaxHeight;
        }), Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.hangingRootRadius;
        }), Codec.intRange(0, 16).fieldOf("hanging_roots_vertical_span").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.hangingRootsVerticalSpan;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.hangingRootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("hanging_root_placement_attempts").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.hangingRootPlacementAttempts;
        }), Codec.intRange(1, 64).fieldOf("allowed_vertical_water_for_tree").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.allowedVerticalWaterForTree;
        }), BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter((rootSystemConfiguration) -> {
            return rootSystemConfiguration.allowedTreePosition;
        })).apply(instance, RootSystemConfiguration::new);
    });
    public final Supplier<PlacedFeature> treeFeature;
    public final int requiredVerticalSpaceForTree;
    public final int rootRadius;
    public final MinecraftKey rootReplaceable;
    public final WorldGenFeatureStateProvider rootStateProvider;
    public final int rootPlacementAttempts;
    public final int rootColumnMaxHeight;
    public final int hangingRootRadius;
    public final int hangingRootsVerticalSpan;
    public final WorldGenFeatureStateProvider hangingRootStateProvider;
    public final int hangingRootPlacementAttempts;
    public final int allowedVerticalWaterForTree;
    public final BlockPredicate allowedTreePosition;

    public RootSystemConfiguration(Supplier<PlacedFeature> feature, int requiredVerticalSpaceForTree, int rootRadius, MinecraftKey rootReplaceable, WorldGenFeatureStateProvider rootStateProvider, int rootPlacementAttempts, int maxRootColumnHeight, int hangingRootRadius, int hangingRootVerticalSpan, WorldGenFeatureStateProvider hangingRootStateProvider, int hangingRootPlacementAttempts, int allowedVerticalWaterForTree, BlockPredicate predicate) {
        this.treeFeature = feature;
        this.requiredVerticalSpaceForTree = requiredVerticalSpaceForTree;
        this.rootRadius = rootRadius;
        this.rootReplaceable = rootReplaceable;
        this.rootStateProvider = rootStateProvider;
        this.rootPlacementAttempts = rootPlacementAttempts;
        this.rootColumnMaxHeight = maxRootColumnHeight;
        this.hangingRootRadius = hangingRootRadius;
        this.hangingRootsVerticalSpan = hangingRootVerticalSpan;
        this.hangingRootStateProvider = hangingRootStateProvider;
        this.hangingRootPlacementAttempts = hangingRootPlacementAttempts;
        this.allowedVerticalWaterForTree = allowedVerticalWaterForTree;
        this.allowedTreePosition = predicate;
    }
}
