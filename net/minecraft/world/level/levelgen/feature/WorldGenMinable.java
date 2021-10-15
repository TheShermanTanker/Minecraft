package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOreConfiguration;

public class WorldGenMinable extends WorldGenerator<WorldGenFeatureOreConfiguration> {
    public WorldGenMinable(Codec<WorldGenFeatureOreConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureOreConfiguration> context) {
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        WorldGenFeatureOreConfiguration oreConfiguration = context.config();
        float f = random.nextFloat() * (float)Math.PI;
        float g = (float)oreConfiguration.size / 8.0F;
        int i = MathHelper.ceil(((float)oreConfiguration.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d = (double)blockPos.getX() + Math.sin((double)f) * (double)g;
        double e = (double)blockPos.getX() - Math.sin((double)f) * (double)g;
        double h = (double)blockPos.getZ() + Math.cos((double)f) * (double)g;
        double j = (double)blockPos.getZ() - Math.cos((double)f) * (double)g;
        int k = 2;
        double l = (double)(blockPos.getY() + random.nextInt(3) - 2);
        double m = (double)(blockPos.getY() + random.nextInt(3) - 2);
        int n = blockPos.getX() - MathHelper.ceil(g) - i;
        int o = blockPos.getY() - 2 - i;
        int p = blockPos.getZ() - MathHelper.ceil(g) - i;
        int q = 2 * (MathHelper.ceil(g) + i);
        int r = 2 * (2 + i);

        for(int s = n; s <= n + q; ++s) {
            for(int t = p; t <= p + q; ++t) {
                if (o <= worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, s, t)) {
                    return this.doPlace(worldGenLevel, random, oreConfiguration, d, e, h, j, l, m, n, o, p, q, r);
                }
            }
        }

        return false;
    }

    protected boolean doPlace(GeneratorAccessSeed worldGenLevel, Random random, WorldGenFeatureOreConfiguration config, double startX, double endX, double startZ, double endZ, double startY, double endY, int x, int y, int z, int horizontalSize, int verticalSize) {
        int i = 0;
        BitSet bitSet = new BitSet(horizontalSize * verticalSize * horizontalSize);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int j = config.size;
        double[] ds = new double[j * 4];

        for(int k = 0; k < j; ++k) {
            float f = (float)k / (float)j;
            double d = MathHelper.lerp((double)f, startX, endX);
            double e = MathHelper.lerp((double)f, startY, endY);
            double g = MathHelper.lerp((double)f, startZ, endZ);
            double h = random.nextDouble() * (double)j / 16.0D;
            double l = ((double)(MathHelper.sin((float)Math.PI * f) + 1.0F) * h + 1.0D) / 2.0D;
            ds[k * 4 + 0] = d;
            ds[k * 4 + 1] = e;
            ds[k * 4 + 2] = g;
            ds[k * 4 + 3] = l;
        }

        for(int m = 0; m < j - 1; ++m) {
            if (!(ds[m * 4 + 3] <= 0.0D)) {
                for(int n = m + 1; n < j; ++n) {
                    if (!(ds[n * 4 + 3] <= 0.0D)) {
                        double o = ds[m * 4 + 0] - ds[n * 4 + 0];
                        double p = ds[m * 4 + 1] - ds[n * 4 + 1];
                        double q = ds[m * 4 + 2] - ds[n * 4 + 2];
                        double r = ds[m * 4 + 3] - ds[n * 4 + 3];
                        if (r * r > o * o + p * p + q * q) {
                            if (r > 0.0D) {
                                ds[n * 4 + 3] = -1.0D;
                            } else {
                                ds[m * 4 + 3] = -1.0D;
                            }
                        }
                    }
                }
            }
        }

        BulkSectionAccess bulkSectionAccess = new BulkSectionAccess(worldGenLevel);

        try {
            for(int s = 0; s < j; ++s) {
                double t = ds[s * 4 + 3];
                if (!(t < 0.0D)) {
                    double u = ds[s * 4 + 0];
                    double v = ds[s * 4 + 1];
                    double w = ds[s * 4 + 2];
                    int aa = Math.max(MathHelper.floor(u - t), x);
                    int ab = Math.max(MathHelper.floor(v - t), y);
                    int ac = Math.max(MathHelper.floor(w - t), z);
                    int ad = Math.max(MathHelper.floor(u + t), aa);
                    int ae = Math.max(MathHelper.floor(v + t), ab);
                    int af = Math.max(MathHelper.floor(w + t), ac);

                    for(int ag = aa; ag <= ad; ++ag) {
                        double ah = ((double)ag + 0.5D - u) / t;
                        if (ah * ah < 1.0D) {
                            for(int ai = ab; ai <= ae; ++ai) {
                                double aj = ((double)ai + 0.5D - v) / t;
                                if (ah * ah + aj * aj < 1.0D) {
                                    for(int ak = ac; ak <= af; ++ak) {
                                        double al = ((double)ak + 0.5D - w) / t;
                                        if (ah * ah + aj * aj + al * al < 1.0D && !worldGenLevel.isOutsideBuildHeight(ai)) {
                                            int am = ag - x + (ai - y) * horizontalSize + (ak - z) * horizontalSize * verticalSize;
                                            if (!bitSet.get(am)) {
                                                bitSet.set(am);
                                                mutableBlockPos.set(ag, ai, ak);
                                                if (worldGenLevel.ensureCanWrite(mutableBlockPos)) {
                                                    ChunkSection levelChunkSection = bulkSectionAccess.getSection(mutableBlockPos);
                                                    if (levelChunkSection != Chunk.EMPTY_SECTION) {
                                                        int an = SectionPosition.sectionRelative(ag);
                                                        int ao = SectionPosition.sectionRelative(ai);
                                                        int ap = SectionPosition.sectionRelative(ak);
                                                        IBlockData blockState = levelChunkSection.getType(an, ao, ap);

                                                        for(WorldGenFeatureOreConfiguration.TargetBlockState targetBlockState : config.targetStates) {
                                                            if (canPlaceOre(blockState, bulkSectionAccess::getBlockState, random, config, targetBlockState, mutableBlockPos)) {
                                                                levelChunkSection.setType(an, ao, ap, targetBlockState.state, false);
                                                                ++i;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable var60) {
            try {
                bulkSectionAccess.close();
            } catch (Throwable var59) {
                var60.addSuppressed(var59);
            }

            throw var60;
        }

        bulkSectionAccess.close();
        return i > 0;
    }

    public static boolean canPlaceOre(IBlockData state, Function<BlockPosition, IBlockData> posToState, Random random, WorldGenFeatureOreConfiguration config, WorldGenFeatureOreConfiguration.TargetBlockState target, BlockPosition.MutableBlockPosition pos) {
        if (!target.target.test(state, random)) {
            return false;
        } else if (shouldSkipAirCheck(random, config.discardChanceOnAirExposure)) {
            return true;
        } else {
            return !isAdjacentToAir(posToState, pos);
        }
    }

    protected static boolean shouldSkipAirCheck(Random random, float chance) {
        if (chance <= 0.0F) {
            return true;
        } else if (chance >= 1.0F) {
            return false;
        } else {
            return random.nextFloat() >= chance;
        }
    }
}
