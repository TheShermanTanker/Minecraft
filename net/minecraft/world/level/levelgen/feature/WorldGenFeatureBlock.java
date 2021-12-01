package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockTallPlant;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureBlockConfiguration;

public class WorldGenFeatureBlock extends WorldGenerator<WorldGenFeatureBlockConfiguration> {
    public WorldGenFeatureBlock(Codec<WorldGenFeatureBlockConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureBlockConfiguration> context) {
        WorldGenFeatureBlockConfiguration simpleBlockConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        IBlockData blockState = simpleBlockConfiguration.toPlace().getState(context.random(), blockPos);
        if (blockState.canPlace(worldGenLevel, blockPos)) {
            if (blockState.getBlock() instanceof BlockTallPlant) {
                if (!worldGenLevel.isEmpty(blockPos.above())) {
                    return false;
                }

                BlockTallPlant.placeAt(worldGenLevel, blockState, blockPos, 2);
            } else {
                worldGenLevel.setTypeAndData(blockPos, blockState, 2);
            }

            return true;
        } else {
            return false;
        }
    }
}
