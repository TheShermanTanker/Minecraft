package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureMushroomConfiguration;

public abstract class WorldGenMushrooms extends WorldGenerator<WorldGenFeatureMushroomConfiguration> {
    public WorldGenMushrooms(Codec<WorldGenFeatureMushroomConfiguration> configCodec) {
        super(configCodec);
    }

    protected void placeTrunk(GeneratorAccess world, Random random, BlockPosition pos, WorldGenFeatureMushroomConfiguration config, int height, BlockPosition.MutableBlockPosition mutableBlockPos) {
        for(int i = 0; i < height; ++i) {
            mutableBlockPos.set(pos).move(EnumDirection.UP, i);
            if (!world.getType(mutableBlockPos).isSolidRender(world, mutableBlockPos)) {
                this.setBlock(world, mutableBlockPos, config.stemProvider.getState(random, pos));
            }
        }

    }

    protected int getTreeHeight(Random random) {
        int i = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            i *= 2;
        }

        return i;
    }

    protected boolean isValidPosition(GeneratorAccess world, BlockPosition pos, int height, BlockPosition.MutableBlockPosition mutableBlockPos, WorldGenFeatureMushroomConfiguration config) {
        int i = pos.getY();
        if (i >= world.getMinBuildHeight() + 1 && i + height + 1 < world.getMaxBuildHeight()) {
            IBlockData blockState = world.getType(pos.below());
            if (!isDirt(blockState) && !blockState.is(TagsBlock.MUSHROOM_GROW_BLOCK)) {
                return false;
            } else {
                for(int j = 0; j <= height; ++j) {
                    int k = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius, j);

                    for(int l = -k; l <= k; ++l) {
                        for(int m = -k; m <= k; ++m) {
                            IBlockData blockState2 = world.getType(mutableBlockPos.setWithOffset(pos, l, j, m));
                            if (!blockState2.isAir() && !blockState2.is(TagsBlock.LEAVES)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureMushroomConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        WorldGenFeatureMushroomConfiguration hugeMushroomFeatureConfiguration = context.config();
        int i = this.getTreeHeight(random);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        if (!this.isValidPosition(worldGenLevel, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration)) {
            return false;
        } else {
            this.makeCap(worldGenLevel, random, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration);
            this.placeTrunk(worldGenLevel, random, blockPos, hugeMushroomFeatureConfiguration, i, mutableBlockPos);
            return true;
        }
    }

    protected abstract int getTreeRadiusForHeight(int i, int j, int capSize, int y);

    protected abstract void makeCap(GeneratorAccess world, Random random, BlockPosition start, int y, BlockPosition.MutableBlockPosition mutable, WorldGenFeatureMushroomConfiguration config);
}
