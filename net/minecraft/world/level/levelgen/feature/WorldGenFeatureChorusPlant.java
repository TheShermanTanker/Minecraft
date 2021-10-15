package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockChorusFlower;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureChorusPlant extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureChorusPlant(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        if (worldGenLevel.isEmpty(blockPos) && worldGenLevel.getType(blockPos.below()).is(Blocks.END_STONE)) {
            BlockChorusFlower.generatePlant(worldGenLevel, blockPos, random, 8);
            return true;
        } else {
            return false;
        }
    }
}
