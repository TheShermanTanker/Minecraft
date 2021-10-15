package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureBlockPileConfiguration;

public class WorldGenFeatureBlockPile extends WorldGenerator<WorldGenFeatureBlockPileConfiguration> {
    public WorldGenFeatureBlockPile(Codec<WorldGenFeatureBlockPileConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureBlockPileConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        WorldGenFeatureBlockPileConfiguration blockPileConfiguration = context.config();
        if (blockPos.getY() < worldGenLevel.getMinBuildHeight() + 5) {
            return false;
        } else {
            int i = 2 + random.nextInt(2);
            int j = 2 + random.nextInt(2);

            for(BlockPosition blockPos2 : BlockPosition.betweenClosed(blockPos.offset(-i, 0, -j), blockPos.offset(i, 1, j))) {
                int k = blockPos.getX() - blockPos2.getX();
                int l = blockPos.getZ() - blockPos2.getZ();
                if ((float)(k * k + l * l) <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F) {
                    this.tryPlaceBlock(worldGenLevel, blockPos2, random, blockPileConfiguration);
                } else if ((double)random.nextFloat() < 0.031D) {
                    this.tryPlaceBlock(worldGenLevel, blockPos2, random, blockPileConfiguration);
                }
            }

            return true;
        }
    }

    private boolean mayPlaceOn(GeneratorAccess world, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        return blockState.is(Blocks.DIRT_PATH) ? random.nextBoolean() : blockState.isFaceSturdy(world, blockPos, EnumDirection.UP);
    }

    private void tryPlaceBlock(GeneratorAccess world, BlockPosition pos, Random random, WorldGenFeatureBlockPileConfiguration config) {
        if (world.isEmpty(pos) && this.mayPlaceOn(world, pos, random)) {
            world.setTypeAndData(pos, config.stateProvider.getState(random, pos), 4);
        }

    }
}
