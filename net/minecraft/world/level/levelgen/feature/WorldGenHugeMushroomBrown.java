package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockHugeMushroom;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureMushroomConfiguration;

public class WorldGenHugeMushroomBrown extends WorldGenMushrooms {
    public WorldGenHugeMushroomBrown(Codec<WorldGenFeatureMushroomConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected void makeCap(GeneratorAccess world, Random random, BlockPosition start, int y, BlockPosition.MutableBlockPosition mutable, WorldGenFeatureMushroomConfiguration config) {
        int i = config.foliageRadius;

        for(int j = -i; j <= i; ++j) {
            for(int k = -i; k <= i; ++k) {
                boolean bl = j == -i;
                boolean bl2 = j == i;
                boolean bl3 = k == -i;
                boolean bl4 = k == i;
                boolean bl5 = bl || bl2;
                boolean bl6 = bl3 || bl4;
                if (!bl5 || !bl6) {
                    mutable.setWithOffset(start, j, y, k);
                    if (!world.getType(mutable).isSolidRender(world, mutable)) {
                        boolean bl7 = bl || bl6 && j == 1 - i;
                        boolean bl8 = bl2 || bl6 && j == i - 1;
                        boolean bl9 = bl3 || bl5 && k == 1 - i;
                        boolean bl10 = bl4 || bl5 && k == i - 1;
                        IBlockData blockState = config.capProvider.getState(random, start);
                        if (blockState.hasProperty(BlockHugeMushroom.WEST) && blockState.hasProperty(BlockHugeMushroom.EAST) && blockState.hasProperty(BlockHugeMushroom.NORTH) && blockState.hasProperty(BlockHugeMushroom.SOUTH)) {
                            blockState = blockState.set(BlockHugeMushroom.WEST, Boolean.valueOf(bl7)).set(BlockHugeMushroom.EAST, Boolean.valueOf(bl8)).set(BlockHugeMushroom.NORTH, Boolean.valueOf(bl9)).set(BlockHugeMushroom.SOUTH, Boolean.valueOf(bl10));
                        }

                        this.setBlock(world, mutable, blockState);
                    }
                }
            }
        }

    }

    @Override
    protected int getTreeRadiusForHeight(int i, int j, int capSize, int y) {
        return y <= 3 ? 0 : capSize;
    }
}
