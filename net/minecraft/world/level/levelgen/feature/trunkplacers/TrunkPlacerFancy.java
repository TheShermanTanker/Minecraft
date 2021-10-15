package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.BlockRotatable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenTrees;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerFancy extends TrunkPlacer {
    public static final Codec<TrunkPlacerFancy> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerFancy::new);
    });
    private static final double TRUNK_HEIGHT_SCALE = 0.618D;
    private static final double CLUSTER_DENSITY_MAGIC = 1.382D;
    private static final double BRANCH_SLOPE = 0.381D;
    private static final double BRANCH_LENGTH_MAGIC = 0.328D;

    public TrunkPlacerFancy(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.FANCY_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        int i = 5;
        int j = height + 2;
        int k = MathHelper.floor((double)j * 0.618D);
        setDirtAt(world, replacer, random, startPos.below(), config);
        double d = 1.0D;
        int l = Math.min(1, MathHelper.floor(1.382D + Math.pow(1.0D * (double)j / 13.0D, 2.0D)));
        int m = startPos.getY() + k;
        int n = j - 5;
        List<TrunkPlacerFancy.FoliageCoords> list = Lists.newArrayList();
        list.add(new TrunkPlacerFancy.FoliageCoords(startPos.above(n), m));

        for(; n >= 0; --n) {
            float f = treeShape(j, n);
            if (!(f < 0.0F)) {
                for(int o = 0; o < l; ++o) {
                    double e = 1.0D;
                    double g = 1.0D * (double)f * ((double)random.nextFloat() + 0.328D);
                    double h = (double)(random.nextFloat() * 2.0F) * Math.PI;
                    double p = g * Math.sin(h) + 0.5D;
                    double q = g * Math.cos(h) + 0.5D;
                    BlockPosition blockPos = startPos.offset(p, (double)(n - 1), q);
                    BlockPosition blockPos2 = blockPos.above(5);
                    if (this.makeLimb(world, replacer, random, blockPos, blockPos2, false, config)) {
                        int r = startPos.getX() - blockPos.getX();
                        int s = startPos.getZ() - blockPos.getZ();
                        double t = (double)blockPos.getY() - Math.sqrt((double)(r * r + s * s)) * 0.381D;
                        int u = t > (double)m ? m : (int)t;
                        BlockPosition blockPos3 = new BlockPosition(startPos.getX(), u, startPos.getZ());
                        if (this.makeLimb(world, replacer, random, blockPos3, blockPos, false, config)) {
                            list.add(new TrunkPlacerFancy.FoliageCoords(blockPos, blockPos3.getY()));
                        }
                    }
                }
            }
        }

        this.makeLimb(world, replacer, random, startPos, startPos.above(k), true, config);
        this.makeBranches(world, replacer, random, j, startPos, list, config);
        List<WorldGenFoilagePlacer.FoliageAttachment> list2 = Lists.newArrayList();

        for(TrunkPlacerFancy.FoliageCoords foliageCoords : list) {
            if (this.trimBranches(j, foliageCoords.getBranchBase() - startPos.getY())) {
                list2.add(foliageCoords.attachment);
            }
        }

        return list2;
    }

    private boolean makeLimb(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition startPos, BlockPosition branchPos, boolean make, WorldGenFeatureTreeConfiguration config) {
        if (!make && Objects.equals(startPos, branchPos)) {
            return true;
        } else {
            BlockPosition blockPos = branchPos.offset(-startPos.getX(), -startPos.getY(), -startPos.getZ());
            int i = this.getSteps(blockPos);
            float f = (float)blockPos.getX() / (float)i;
            float g = (float)blockPos.getY() / (float)i;
            float h = (float)blockPos.getZ() / (float)i;

            for(int j = 0; j <= i; ++j) {
                BlockPosition blockPos2 = startPos.offset((double)(0.5F + (float)j * f), (double)(0.5F + (float)j * g), (double)(0.5F + (float)j * h));
                if (make) {
                    TrunkPlacer.placeLog(world, replacer, random, blockPos2, config, (state) -> {
                        return state.set(BlockRotatable.AXIS, this.getLogAxis(startPos, blockPos2));
                    });
                } else if (!WorldGenTrees.isFree(world, blockPos2)) {
                    return false;
                }
            }

            return true;
        }
    }

    private int getSteps(BlockPosition offset) {
        int i = MathHelper.abs(offset.getX());
        int j = MathHelper.abs(offset.getY());
        int k = MathHelper.abs(offset.getZ());
        return Math.max(i, Math.max(j, k));
    }

    private EnumDirection.EnumAxis getLogAxis(BlockPosition branchStart, BlockPosition branchEnd) {
        EnumDirection.EnumAxis axis = EnumDirection.EnumAxis.Y;
        int i = Math.abs(branchEnd.getX() - branchStart.getX());
        int j = Math.abs(branchEnd.getZ() - branchStart.getZ());
        int k = Math.max(i, j);
        if (k > 0) {
            if (i == k) {
                axis = EnumDirection.EnumAxis.X;
            } else {
                axis = EnumDirection.EnumAxis.Z;
            }
        }

        return axis;
    }

    private boolean trimBranches(int treeHeight, int height) {
        return (double)height >= (double)treeHeight * 0.2D;
    }

    private void makeBranches(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int treeHeight, BlockPosition startPos, List<TrunkPlacerFancy.FoliageCoords> branchPositions, WorldGenFeatureTreeConfiguration config) {
        for(TrunkPlacerFancy.FoliageCoords foliageCoords : branchPositions) {
            int i = foliageCoords.getBranchBase();
            BlockPosition blockPos = new BlockPosition(startPos.getX(), i, startPos.getZ());
            if (!blockPos.equals(foliageCoords.attachment.pos()) && this.trimBranches(treeHeight, i - startPos.getY())) {
                this.makeLimb(world, replacer, random, blockPos, foliageCoords.attachment.pos(), true, config);
            }
        }

    }

    private static float treeShape(int treeHeight, int height) {
        if ((float)height < (float)treeHeight * 0.3F) {
            return -1.0F;
        } else {
            float f = (float)treeHeight / 2.0F;
            float g = f - (float)height;
            float h = MathHelper.sqrt(f * f - g * g);
            if (g == 0.0F) {
                h = f;
            } else if (Math.abs(g) >= f) {
                return 0.0F;
            }

            return h * 0.5F;
        }
    }

    static class FoliageCoords {
        final WorldGenFoilagePlacer.FoliageAttachment attachment;
        private final int branchBase;

        public FoliageCoords(BlockPosition pos, int width) {
            this.attachment = new WorldGenFoilagePlacer.FoliageAttachment(pos, 0, false);
            this.branchBase = width;
        }

        public int getBranchBase() {
            return this.branchBase;
        }
    }
}
