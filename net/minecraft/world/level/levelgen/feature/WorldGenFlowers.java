package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;

public abstract class WorldGenFlowers<U extends WorldGenFeatureConfiguration> extends WorldGenerator<U> {
    public WorldGenFlowers(Codec<U> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<U> context) {
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        U featureConfiguration = context.config();
        IBlockData blockState = this.getRandomFlower(random, blockPos, featureConfiguration);
        int i = 0;

        for(int j = 0; j < this.getCount(featureConfiguration); ++j) {
            BlockPosition blockPos2 = this.getPos(random, blockPos, featureConfiguration);
            if (worldGenLevel.isEmpty(blockPos2) && blockState.canPlace(worldGenLevel, blockPos2) && this.isValid(worldGenLevel, blockPos2, featureConfiguration)) {
                worldGenLevel.setTypeAndData(blockPos2, blockState, 2);
                ++i;
            }
        }

        return i > 0;
    }

    public abstract boolean isValid(GeneratorAccess world, BlockPosition pos, U config);

    public abstract int getCount(U config);

    public abstract BlockPosition getPos(Random random, BlockPosition pos, U config);

    public abstract IBlockData getRandomFlower(Random random, BlockPosition pos, U config);
}
