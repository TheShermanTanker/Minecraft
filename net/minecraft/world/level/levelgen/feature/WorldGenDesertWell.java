package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenDesertWell extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
    private final IBlockData sandSlab = Blocks.SANDSTONE_SLAB.getBlockData();
    private final IBlockData sandstone = Blocks.SANDSTONE.getBlockData();
    private final IBlockData water = Blocks.WATER.getBlockData();

    public WorldGenDesertWell(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();

        for(blockPos = blockPos.above(); worldGenLevel.isEmpty(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        if (!IS_SAND.test(worldGenLevel.getType(blockPos))) {
            return false;
        } else {
            for(int i = -2; i <= 2; ++i) {
                for(int j = -2; j <= 2; ++j) {
                    if (worldGenLevel.isEmpty(blockPos.offset(i, -1, j)) && worldGenLevel.isEmpty(blockPos.offset(i, -2, j))) {
                        return false;
                    }
                }
            }

            for(int k = -1; k <= 0; ++k) {
                for(int l = -2; l <= 2; ++l) {
                    for(int m = -2; m <= 2; ++m) {
                        worldGenLevel.setTypeAndData(blockPos.offset(l, k, m), this.sandstone, 2);
                    }
                }
            }

            worldGenLevel.setTypeAndData(blockPos, this.water, 2);

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                worldGenLevel.setTypeAndData(blockPos.relative(direction), this.water, 2);
            }

            for(int n = -2; n <= 2; ++n) {
                for(int o = -2; o <= 2; ++o) {
                    if (n == -2 || n == 2 || o == -2 || o == 2) {
                        worldGenLevel.setTypeAndData(blockPos.offset(n, 1, o), this.sandstone, 2);
                    }
                }
            }

            worldGenLevel.setTypeAndData(blockPos.offset(2, 1, 0), this.sandSlab, 2);
            worldGenLevel.setTypeAndData(blockPos.offset(-2, 1, 0), this.sandSlab, 2);
            worldGenLevel.setTypeAndData(blockPos.offset(0, 1, 2), this.sandSlab, 2);
            worldGenLevel.setTypeAndData(blockPos.offset(0, 1, -2), this.sandSlab, 2);

            for(int p = -1; p <= 1; ++p) {
                for(int q = -1; q <= 1; ++q) {
                    if (p == 0 && q == 0) {
                        worldGenLevel.setTypeAndData(blockPos.offset(p, 4, q), this.sandstone, 2);
                    } else {
                        worldGenLevel.setTypeAndData(blockPos.offset(p, 4, q), this.sandSlab, 2);
                    }
                }
            }

            for(int r = 1; r <= 3; ++r) {
                worldGenLevel.setTypeAndData(blockPos.offset(-1, r, -1), this.sandstone, 2);
                worldGenLevel.setTypeAndData(blockPos.offset(-1, r, 1), this.sandstone, 2);
                worldGenLevel.setTypeAndData(blockPos.offset(1, r, -1), this.sandstone, 2);
                worldGenLevel.setTypeAndData(blockPos.offset(1, r, 1), this.sandstone, 2);
            }

            return true;
        }
    }
}
