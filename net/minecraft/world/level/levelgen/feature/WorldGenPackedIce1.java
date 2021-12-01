package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureCircleConfiguration;

public class WorldGenPackedIce1 extends WorldGenFeatureDisk {
    public WorldGenPackedIce1(Codec<WorldGenFeatureCircleConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureCircleConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        Random random = context.random();
        WorldGenFeatureCircleConfiguration diskConfiguration = context.config();

        BlockPosition blockPos;
        for(blockPos = context.origin(); worldGenLevel.isEmpty(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        return !worldGenLevel.getType(blockPos).is(Blocks.SNOW_BLOCK) ? false : super.generate(new FeaturePlaceContext<>(context.topFeature(), worldGenLevel, context.chunkGenerator(), context.random(), blockPos, context.config()));
    }
}
