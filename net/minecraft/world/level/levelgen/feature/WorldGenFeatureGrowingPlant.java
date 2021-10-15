package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.GrowingPlantConfiguration;

public class WorldGenFeatureGrowingPlant extends WorldGenerator<GrowingPlantConfiguration> {
    public WorldGenFeatureGrowingPlant(Codec<GrowingPlantConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<GrowingPlantConfiguration> context) {
        GeneratorAccess levelAccessor = context.level();
        GrowingPlantConfiguration growingPlantConfiguration = context.config();
        Random random = context.random();
        int i = growingPlantConfiguration.heightDistribution.getRandomValue(random).orElseThrow(IllegalStateException::new).sample(random);
        BlockPosition.MutableBlockPosition mutableBlockPos = context.origin().mutable();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = mutableBlockPos.mutable().move(growingPlantConfiguration.direction);
        IBlockData blockState = levelAccessor.getType(mutableBlockPos);

        for(int j = 1; j <= i; ++j) {
            IBlockData blockState2 = blockState;
            blockState = levelAccessor.getType(mutableBlockPos2);
            if (blockState2.isAir() || growingPlantConfiguration.allowWater && blockState2.getFluid().is(TagsFluid.WATER)) {
                if (j == i || !blockState.isAir()) {
                    levelAccessor.setTypeAndData(mutableBlockPos, growingPlantConfiguration.headProvider.getState(random, mutableBlockPos), 2);
                    break;
                }

                levelAccessor.setTypeAndData(mutableBlockPos, growingPlantConfiguration.bodyProvider.getState(random, mutableBlockPos), 2);
            }

            mutableBlockPos2.move(growingPlantConfiguration.direction);
            mutableBlockPos.move(growingPlantConfiguration.direction);
        }

        return true;
    }
}
