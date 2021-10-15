package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureBlockPileConfiguration;

public class WorldGenFeatureNetherForestVegetation extends WorldGenerator<WorldGenFeatureBlockPileConfiguration> {
    public WorldGenFeatureNetherForestVegetation(Codec<WorldGenFeatureBlockPileConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureBlockPileConfiguration> context) {
        return place(context.level(), context.random(), context.origin(), context.config(), 8, 4);
    }

    public static boolean place(GeneratorAccess world, Random random, BlockPosition pos, WorldGenFeatureBlockPileConfiguration config, int i, int j) {
        IBlockData blockState = world.getType(pos.below());
        if (!blockState.is(TagsBlock.NYLIUM)) {
            return false;
        } else {
            int k = pos.getY();
            if (k >= world.getMinBuildHeight() + 1 && k + 1 < world.getMaxBuildHeight()) {
                int l = 0;

                for(int m = 0; m < i * i; ++m) {
                    BlockPosition blockPos = pos.offset(random.nextInt(i) - random.nextInt(i), random.nextInt(j) - random.nextInt(j), random.nextInt(i) - random.nextInt(i));
                    IBlockData blockState2 = config.stateProvider.getState(random, blockPos);
                    if (world.isEmpty(blockPos) && blockPos.getY() > world.getMinBuildHeight() && blockState2.canPlace(world, blockPos)) {
                        world.setTypeAndData(blockPos, blockState2, 2);
                        ++l;
                    }
                }

                return l > 0;
            } else {
                return false;
            }
        }
    }
}
