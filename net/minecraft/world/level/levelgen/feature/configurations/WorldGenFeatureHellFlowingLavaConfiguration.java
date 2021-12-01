package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class WorldGenFeatureHellFlowingLavaConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureHellFlowingLavaConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Fluid.CODEC.fieldOf("state").forGetter((springConfiguration) -> {
            return springConfiguration.state;
        }), Codec.BOOL.fieldOf("requires_block_below").orElse(true).forGetter((springConfiguration) -> {
            return springConfiguration.requiresBlockBelow;
        }), Codec.INT.fieldOf("rock_count").orElse(4).forGetter((springConfiguration) -> {
            return springConfiguration.rockCount;
        }), Codec.INT.fieldOf("hole_count").orElse(1).forGetter((springConfiguration) -> {
            return springConfiguration.holeCount;
        }), IRegistry.BLOCK.byNameCodec().listOf().fieldOf("valid_blocks").xmap(ImmutableSet::copyOf, ImmutableList::copyOf).forGetter((springConfiguration) -> {
            return springConfiguration.validBlocks;
        })).apply(instance, WorldGenFeatureHellFlowingLavaConfiguration::new);
    });
    public final Fluid state;
    public final boolean requiresBlockBelow;
    public final int rockCount;
    public final int holeCount;
    public final Set<Block> validBlocks;

    public WorldGenFeatureHellFlowingLavaConfiguration(Fluid state, boolean requiresBlockBelow, int rockCount, int holeCount, Set<Block> validBlocks) {
        this.state = state;
        this.requiresBlockBelow = requiresBlockBelow;
        this.rockCount = rockCount;
        this.holeCount = holeCount;
        this.validBlocks = validBlocks;
    }
}
