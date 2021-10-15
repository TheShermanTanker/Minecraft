package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenPackedIce2 extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenPackedIce2(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        BlockPosition blockPos = context.origin();
        Random random = context.random();

        GeneratorAccessSeed worldGenLevel;
        for(worldGenLevel = context.level(); worldGenLevel.isEmpty(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        if (!worldGenLevel.getType(blockPos).is(Blocks.SNOW_BLOCK)) {
            return false;
        } else {
            blockPos = blockPos.above(random.nextInt(4));
            int i = random.nextInt(4) + 7;
            int j = i / 4 + random.nextInt(2);
            if (j > 1 && random.nextInt(60) == 0) {
                blockPos = blockPos.above(10 + random.nextInt(30));
            }

            for(int k = 0; k < i; ++k) {
                float f = (1.0F - (float)k / (float)i) * (float)j;
                int l = MathHelper.ceil(f);

                for(int m = -l; m <= l; ++m) {
                    float g = (float)MathHelper.abs(m) - 0.25F;

                    for(int n = -l; n <= l; ++n) {
                        float h = (float)MathHelper.abs(n) - 0.25F;
                        if ((m == 0 && n == 0 || !(g * g + h * h > f * f)) && (m != -l && m != l && n != -l && n != l || !(random.nextFloat() > 0.75F))) {
                            IBlockData blockState = worldGenLevel.getType(blockPos.offset(m, k, n));
                            if (blockState.isAir() || isDirt(blockState) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.ICE)) {
                                this.setBlock(worldGenLevel, blockPos.offset(m, k, n), Blocks.PACKED_ICE.getBlockData());
                            }

                            if (k != 0 && l > 1) {
                                blockState = worldGenLevel.getType(blockPos.offset(m, -k, n));
                                if (blockState.isAir() || isDirt(blockState) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.ICE)) {
                                    this.setBlock(worldGenLevel, blockPos.offset(m, -k, n), Blocks.PACKED_ICE.getBlockData());
                                }
                            }
                        }
                    }
                }
            }

            int o = j - 1;
            if (o < 0) {
                o = 0;
            } else if (o > 1) {
                o = 1;
            }

            for(int p = -o; p <= o; ++p) {
                for(int q = -o; q <= o; ++q) {
                    BlockPosition blockPos2 = blockPos.offset(p, -1, q);
                    int r = 50;
                    if (Math.abs(p) == 1 && Math.abs(q) == 1) {
                        r = random.nextInt(5);
                    }

                    while(blockPos2.getY() > 50) {
                        IBlockData blockState2 = worldGenLevel.getType(blockPos2);
                        if (!blockState2.isAir() && !isDirt(blockState2) && !blockState2.is(Blocks.SNOW_BLOCK) && !blockState2.is(Blocks.ICE) && !blockState2.is(Blocks.PACKED_ICE)) {
                            break;
                        }

                        this.setBlock(worldGenLevel, blockPos2, Blocks.PACKED_ICE.getBlockData());
                        blockPos2 = blockPos2.below();
                        --r;
                        if (r <= 0) {
                            blockPos2 = blockPos2.below(random.nextInt(5) + 1);
                            r = random.nextInt(5);
                        }
                    }
                }
            }

            return true;
        }
    }
}
