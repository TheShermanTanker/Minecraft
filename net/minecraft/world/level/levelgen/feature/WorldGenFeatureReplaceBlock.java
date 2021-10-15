package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureReplaceBlockConfiguration;

public class WorldGenFeatureReplaceBlock extends WorldGenerator<WorldGenFeatureReplaceBlockConfiguration> {
    public WorldGenFeatureReplaceBlock(Codec<WorldGenFeatureReplaceBlockConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureReplaceBlockConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        WorldGenFeatureReplaceBlockConfiguration replaceBlockConfiguration = context.config();

        for(WorldGenFeatureOreConfiguration.TargetBlockState targetBlockState : replaceBlockConfiguration.targetStates) {
            if (targetBlockState.target.test(worldGenLevel.getType(blockPos), context.random())) {
                worldGenLevel.setTypeAndData(blockPos, targetBlockState.state, 2);
                break;
            }
        }

        return true;
    }
}
