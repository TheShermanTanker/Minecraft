package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockHugeMushroom;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureMushroomConfiguration;

public class WorldGenHugeMushroomRed extends WorldGenMushrooms {
    public WorldGenHugeMushroomRed(Codec<WorldGenFeatureMushroomConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected void makeCap(GeneratorAccess world, Random random, BlockPosition start, int y, BlockPosition.MutableBlockPosition mutable, WorldGenFeatureMushroomConfiguration config) {
        for(int i = y - 3; i <= y; ++i) {
            int j = i < y ? config.foliageRadius : config.foliageRadius - 1;
            int k = config.foliageRadius - 2;

            for(int l = -j; l <= j; ++l) {
                for(int m = -j; m <= j; ++m) {
                    boolean bl = l == -j;
                    boolean bl2 = l == j;
                    boolean bl3 = m == -j;
                    boolean bl4 = m == j;
                    boolean bl5 = bl || bl2;
                    boolean bl6 = bl3 || bl4;
                    if (i >= y || bl5 != bl6) {
                        mutable.setWithOffset(start, l, i, m);
                        if (!world.getType(mutable).isSolidRender(world, mutable)) {
                            IBlockData blockState = config.capProvider.getState(random, start);
                            if (blockState.hasProperty(BlockHugeMushroom.WEST) && blockState.hasProperty(BlockHugeMushroom.EAST) && blockState.hasProperty(BlockHugeMushroom.NORTH) && blockState.hasProperty(BlockHugeMushroom.SOUTH) && blockState.hasProperty(BlockHugeMushroom.UP)) {
                                blockState = blockState.set(BlockHugeMushroom.UP, Boolean.valueOf(i >= y - 1)).set(BlockHugeMushroom.WEST, Boolean.valueOf(l < -k)).set(BlockHugeMushroom.EAST, Boolean.valueOf(l > k)).set(BlockHugeMushroom.NORTH, Boolean.valueOf(m < -k)).set(BlockHugeMushroom.SOUTH, Boolean.valueOf(m > k));
                            }

                            this.setBlock(world, mutable, blockState);
                        }
                    }
                }
            }
        }

    }

    @Override
    protected int getTreeRadiusForHeight(int i, int j, int capSize, int y) {
        int k = 0;
        if (y < j && y >= j - 3) {
            k = capSize;
        } else if (y == j) {
            k = capSize;
        }

        return k;
    }
}
