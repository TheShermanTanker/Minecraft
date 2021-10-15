package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockSeaPickle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenDecoratorFrequencyConfiguration;

public class WorldGenFeatureSeaPickel extends WorldGenerator<WorldGenDecoratorFrequencyConfiguration> {
    public WorldGenFeatureSeaPickel(Codec<WorldGenDecoratorFrequencyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenDecoratorFrequencyConfiguration> context) {
        int i = 0;
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        int j = context.config().count().sample(random);

        for(int k = 0; k < j; ++k) {
            int l = random.nextInt(8) - random.nextInt(8);
            int m = random.nextInt(8) - random.nextInt(8);
            int n = worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR, blockPos.getX() + l, blockPos.getZ() + m);
            BlockPosition blockPos2 = new BlockPosition(blockPos.getX() + l, n, blockPos.getZ() + m);
            IBlockData blockState = Blocks.SEA_PICKLE.getBlockData().set(BlockSeaPickle.PICKLES, Integer.valueOf(random.nextInt(4) + 1));
            if (worldGenLevel.getType(blockPos2).is(Blocks.WATER) && blockState.canPlace(worldGenLevel, blockPos2)) {
                worldGenLevel.setTypeAndData(blockPos2, blockState, 2);
                ++i;
            }
        }

        return i > 0;
    }
}
