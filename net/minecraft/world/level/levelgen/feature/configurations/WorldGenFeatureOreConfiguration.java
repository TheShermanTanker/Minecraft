package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureRuleTest;

public class WorldGenFeatureOreConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureOreConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.list(WorldGenFeatureOreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter((oreConfiguration) -> {
            return oreConfiguration.targetStates;
        }), Codec.intRange(0, 64).fieldOf("size").forGetter((oreConfiguration) -> {
            return oreConfiguration.size;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter((oreConfiguration) -> {
            return oreConfiguration.discardChanceOnAirExposure;
        })).apply(instance, WorldGenFeatureOreConfiguration::new);
    });
    public final List<WorldGenFeatureOreConfiguration.TargetBlockState> targetStates;
    public final int size;
    public final float discardChanceOnAirExposure;

    public WorldGenFeatureOreConfiguration(List<WorldGenFeatureOreConfiguration.TargetBlockState> targets, int size, float discardOnAirChance) {
        this.size = size;
        this.targetStates = targets;
        this.discardChanceOnAirExposure = discardOnAirChance;
    }

    public WorldGenFeatureOreConfiguration(List<WorldGenFeatureOreConfiguration.TargetBlockState> targets, int size) {
        this(targets, size, 0.0F);
    }

    public WorldGenFeatureOreConfiguration(DefinedStructureRuleTest test, IBlockData state, int size, float discardOnAirChance) {
        this(ImmutableList.of(new WorldGenFeatureOreConfiguration.TargetBlockState(test, state)), size, discardOnAirChance);
    }

    public WorldGenFeatureOreConfiguration(DefinedStructureRuleTest test, IBlockData state, int size) {
        this(ImmutableList.of(new WorldGenFeatureOreConfiguration.TargetBlockState(test, state)), size, 0.0F);
    }

    public static WorldGenFeatureOreConfiguration.TargetBlockState target(DefinedStructureRuleTest test, IBlockData state) {
        return new WorldGenFeatureOreConfiguration.TargetBlockState(test, state);
    }

    public static class TargetBlockState {
        public static final Codec<WorldGenFeatureOreConfiguration.TargetBlockState> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(DefinedStructureRuleTest.CODEC.fieldOf("target").forGetter((targetBlockState) -> {
                return targetBlockState.target;
            }), IBlockData.CODEC.fieldOf("state").forGetter((targetBlockState) -> {
                return targetBlockState.state;
            })).apply(instance, WorldGenFeatureOreConfiguration.TargetBlockState::new);
        });
        public final DefinedStructureRuleTest target;
        public final IBlockData state;

        TargetBlockState(DefinedStructureRuleTest target, IBlockData state) {
            this.target = target;
            this.state = state;
        }
    }
}
