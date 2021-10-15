package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureHellFlowingLavaConfiguration;

public class WorldGenLiquids extends WorldGenerator<WorldGenFeatureHellFlowingLavaConfiguration> {
    public WorldGenLiquids(Codec<WorldGenFeatureHellFlowingLavaConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureHellFlowingLavaConfiguration> context) {
        WorldGenFeatureHellFlowingLavaConfiguration springConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        if (!springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.above()).getBlock())) {
            return false;
        } else if (springConfiguration.requiresBlockBelow && !springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.below()).getBlock())) {
            return false;
        } else {
            IBlockData blockState = worldGenLevel.getType(blockPos);
            if (!blockState.isAir() && !springConfiguration.validBlocks.contains(blockState.getBlock())) {
                return false;
            } else {
                int i = 0;
                int j = 0;
                if (springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.west()).getBlock())) {
                    ++j;
                }

                if (springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.east()).getBlock())) {
                    ++j;
                }

                if (springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.north()).getBlock())) {
                    ++j;
                }

                if (springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.south()).getBlock())) {
                    ++j;
                }

                if (springConfiguration.validBlocks.contains(worldGenLevel.getType(blockPos.below()).getBlock())) {
                    ++j;
                }

                int k = 0;
                if (worldGenLevel.isEmpty(blockPos.west())) {
                    ++k;
                }

                if (worldGenLevel.isEmpty(blockPos.east())) {
                    ++k;
                }

                if (worldGenLevel.isEmpty(blockPos.north())) {
                    ++k;
                }

                if (worldGenLevel.isEmpty(blockPos.south())) {
                    ++k;
                }

                if (worldGenLevel.isEmpty(blockPos.below())) {
                    ++k;
                }

                if (j == springConfiguration.rockCount && k == springConfiguration.holeCount) {
                    worldGenLevel.setTypeAndData(blockPos, springConfiguration.state.getBlockData(), 2);
                    worldGenLevel.getFluidTickList().scheduleTick(blockPos, springConfiguration.state.getType(), 0);
                    ++i;
                }

                return i > 0;
            }
        }
    }
}
