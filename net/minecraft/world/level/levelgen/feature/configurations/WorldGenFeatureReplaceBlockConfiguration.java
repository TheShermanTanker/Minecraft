package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureTestBlockState;

public class WorldGenFeatureReplaceBlockConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureReplaceBlockConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.list(WorldGenFeatureOreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter((replaceBlockConfiguration) -> {
            return replaceBlockConfiguration.targetStates;
        })).apply(instance, WorldGenFeatureReplaceBlockConfiguration::new);
    });
    public final List<WorldGenFeatureOreConfiguration.TargetBlockState> targetStates;

    public WorldGenFeatureReplaceBlockConfiguration(IBlockData target, IBlockData state) {
        this(ImmutableList.of(WorldGenFeatureOreConfiguration.target(new DefinedStructureTestBlockState(target), state)));
    }

    public WorldGenFeatureReplaceBlockConfiguration(List<WorldGenFeatureOreConfiguration.TargetBlockState> list) {
        this.targetStates = list;
    }
}
