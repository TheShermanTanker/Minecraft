package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;

/** @deprecated */
@Deprecated
public class WorldGenLakes extends WorldGenerator<LakeFeature$Configuration> {
    private static final IBlockData AIR = Blocks.CAVE_AIR.getBlockData();

    public WorldGenLakes(Codec<LakeFeature$Configuration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<LakeFeature$Configuration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        LakeFeature$Configuration configuration = context.config();
        if (blockPos.getY() <= worldGenLevel.getMinBuildHeight() + 4) {
            return false;
        } else {
            blockPos = blockPos.below(4);
            if (!worldGenLevel.startsForFeature(SectionPosition.of(blockPos), StructureGenerator.VILLAGE).isEmpty()) {
                return false;
            } else {
                boolean[] bls = new boolean[2048];
                int i = random.nextInt(4) + 4;

                for(int j = 0; j < i; ++j) {
                    double d = random.nextDouble() * 6.0D + 3.0D;
                    double e = random.nextDouble() * 4.0D + 2.0D;
                    double f = random.nextDouble() * 6.0D + 3.0D;
                    double g = random.nextDouble() * (16.0D - d - 2.0D) + 1.0D + d / 2.0D;
                    double h = random.nextDouble() * (8.0D - e - 4.0D) + 2.0D + e / 2.0D;
                    double k = random.nextDouble() * (16.0D - f - 2.0D) + 1.0D + f / 2.0D;

                    for(int l = 1; l < 15; ++l) {
                        for(int m = 1; m < 15; ++m) {
                            for(int n = 1; n < 7; ++n) {
                                double o = ((double)l - g) / (d / 2.0D);
                                double p = ((double)n - h) / (e / 2.0D);
                                double q = ((double)m - k) / (f / 2.0D);
                                double r = o * o + p * p + q * q;
                                if (r < 1.0D) {
                                    bls[(l * 16 + m) * 8 + n] = true;
                                }
                            }
                        }
                    }
                }

                IBlockData blockState = configuration.fluid().getState(random, blockPos);

                for(int s = 0; s < 16; ++s) {
                    for(int t = 0; t < 16; ++t) {
                        for(int u = 0; u < 8; ++u) {
                            boolean bl = !bls[(s * 16 + t) * 8 + u] && (s < 15 && bls[((s + 1) * 16 + t) * 8 + u] || s > 0 && bls[((s - 1) * 16 + t) * 8 + u] || t < 15 && bls[(s * 16 + t + 1) * 8 + u] || t > 0 && bls[(s * 16 + (t - 1)) * 8 + u] || u < 7 && bls[(s * 16 + t) * 8 + u + 1] || u > 0 && bls[(s * 16 + t) * 8 + (u - 1)]);
                            if (bl) {
                                Material material = worldGenLevel.getType(blockPos.offset(s, u, t)).getMaterial();
                                if (u >= 4 && material.isLiquid()) {
                                    return false;
                                }

                                if (u < 4 && !material.isBuildable() && worldGenLevel.getType(blockPos.offset(s, u, t)) != blockState) {
                                    return false;
                                }
                            }
                        }
                    }
                }

                for(int v = 0; v < 16; ++v) {
                    for(int w = 0; w < 16; ++w) {
                        for(int x = 0; x < 8; ++x) {
                            if (bls[(v * 16 + w) * 8 + x]) {
                                BlockPosition blockPos2 = blockPos.offset(v, x, w);
                                if (this.canReplaceBlock(worldGenLevel.getType(blockPos2))) {
                                    boolean bl2 = x >= 4;
                                    worldGenLevel.setTypeAndData(blockPos2, bl2 ? AIR : blockState, 2);
                                    if (bl2) {
                                        worldGenLevel.scheduleTick(blockPos2, AIR.getBlock(), 0);
                                        this.markAboveForPostProcessing(worldGenLevel, blockPos2);
                                    }
                                }
                            }
                        }
                    }
                }

                IBlockData blockState2 = configuration.barrier().getState(random, blockPos);
                if (!blockState2.isAir()) {
                    for(int y = 0; y < 16; ++y) {
                        for(int z = 0; z < 16; ++z) {
                            for(int aa = 0; aa < 8; ++aa) {
                                boolean bl3 = !bls[(y * 16 + z) * 8 + aa] && (y < 15 && bls[((y + 1) * 16 + z) * 8 + aa] || y > 0 && bls[((y - 1) * 16 + z) * 8 + aa] || z < 15 && bls[(y * 16 + z + 1) * 8 + aa] || z > 0 && bls[(y * 16 + (z - 1)) * 8 + aa] || aa < 7 && bls[(y * 16 + z) * 8 + aa + 1] || aa > 0 && bls[(y * 16 + z) * 8 + (aa - 1)]);
                                if (bl3 && (aa < 4 || random.nextInt(2) != 0)) {
                                    IBlockData blockState3 = worldGenLevel.getType(blockPos.offset(y, aa, z));
                                    if (blockState3.getMaterial().isBuildable() && !blockState3.is(TagsBlock.LAVA_POOL_STONE_CANNOT_REPLACE)) {
                                        BlockPosition blockPos3 = blockPos.offset(y, aa, z);
                                        worldGenLevel.setTypeAndData(blockPos3, blockState2, 2);
                                        this.markAboveForPostProcessing(worldGenLevel, blockPos3);
                                    }
                                }
                            }
                        }
                    }
                }

                if (blockState.getFluid().is(TagsFluid.WATER)) {
                    for(int ab = 0; ab < 16; ++ab) {
                        for(int ac = 0; ac < 16; ++ac) {
                            int ad = 4;
                            BlockPosition blockPos4 = blockPos.offset(ab, 4, ac);
                            if (worldGenLevel.getBiome(blockPos4).shouldFreeze(worldGenLevel, blockPos4, false) && this.canReplaceBlock(worldGenLevel.getType(blockPos4))) {
                                worldGenLevel.setTypeAndData(blockPos4, Blocks.ICE.getBlockData(), 2);
                            }
                        }
                    }
                }

                return true;
            }
        }
    }

    private boolean canReplaceBlock(IBlockData state) {
        return !state.is(TagsBlock.FEATURES_CANNOT_REPLACE);
    }
}
