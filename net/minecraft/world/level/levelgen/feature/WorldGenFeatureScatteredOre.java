package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOreConfiguration;

public class WorldGenFeatureScatteredOre extends WorldGenerator<WorldGenFeatureOreConfiguration> {
    private static final int MAX_DIST_FROM_ORIGIN = 7;

    WorldGenFeatureScatteredOre(Codec<WorldGenFeatureOreConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureOreConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        WorldGenFeatureOreConfiguration oreConfiguration = context.config();
        BlockPosition blockPos = context.origin();
        int i = random.nextInt(oreConfiguration.size + 1);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = 0; j < i; ++j) {
            this.offsetTargetPos(mutableBlockPos, random, blockPos, Math.min(j, 7));
            IBlockData blockState = worldGenLevel.getType(mutableBlockPos);

            for(WorldGenFeatureOreConfiguration.TargetBlockState targetBlockState : oreConfiguration.targetStates) {
                if (WorldGenMinable.canPlaceOre(blockState, worldGenLevel::getType, random, oreConfiguration, targetBlockState, mutableBlockPos)) {
                    worldGenLevel.setTypeAndData(mutableBlockPos, targetBlockState.state, 2);
                    break;
                }
            }
        }

        return true;
    }

    private void offsetTargetPos(BlockPosition.MutableBlockPosition mutable, Random random, BlockPosition origin, int spread) {
        int i = this.getRandomPlacementInOneAxisRelativeToOrigin(random, spread);
        int j = this.getRandomPlacementInOneAxisRelativeToOrigin(random, spread);
        int k = this.getRandomPlacementInOneAxisRelativeToOrigin(random, spread);
        mutable.setWithOffset(origin, i, j, k);
    }

    private int getRandomPlacementInOneAxisRelativeToOrigin(Random random, int spread) {
        return Math.round((random.nextFloat() - random.nextFloat()) * (float)spread);
    }
}
