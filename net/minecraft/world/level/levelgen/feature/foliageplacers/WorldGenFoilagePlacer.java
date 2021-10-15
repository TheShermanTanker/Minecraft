package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenTrees;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public abstract class WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacer> CODEC = IRegistry.FOLIAGE_PLACER_TYPES.dispatch(WorldGenFoilagePlacer::type, WorldGenFoilagePlacers::codec);
    protected final IntProvider radius;
    protected final IntProvider offset;

    protected static <P extends WorldGenFoilagePlacer> P2<Mu<P>, IntProvider, IntProvider> foliagePlacerParts(Instance<P> instance) {
        return instance.group(IntProvider.codec(0, 16).fieldOf("radius").forGetter((placer) -> {
            return placer.radius;
        }), IntProvider.codec(0, 16).fieldOf("offset").forGetter((placer) -> {
            return placer.offset;
        }));
    }

    public WorldGenFoilagePlacer(IntProvider radius, IntProvider offset) {
        this.radius = radius;
        this.offset = offset;
    }

    protected abstract WorldGenFoilagePlacers<?> type();

    public void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius) {
        this.createFoliage(world, replacer, random, config, trunkHeight, treeNode, foliageHeight, radius, this.offset(random));
    }

    protected abstract void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset);

    public abstract int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config);

    public int foliageRadius(Random random, int baseHeight) {
        return this.radius.sample(random);
    }

    private int offset(Random random) {
        return this.offset.sample(random);
    }

    protected abstract boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk);

    protected boolean shouldSkipLocationSigned(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        int i;
        int j;
        if (giantTrunk) {
            i = Math.min(Math.abs(dx), Math.abs(dx - 1));
            j = Math.min(Math.abs(dz), Math.abs(dz - 1));
        } else {
            i = Math.abs(dx);
            j = Math.abs(dz);
        }

        return this.shouldSkipLocation(random, i, y, j, radius, giantTrunk);
    }

    protected void placeLeavesRow(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, BlockPosition centerPos, int radius, int y, boolean giantTrunk) {
        int i = giantTrunk ? 1 : 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = -radius; j <= radius + i; ++j) {
            for(int k = -radius; k <= radius + i; ++k) {
                if (!this.shouldSkipLocationSigned(random, j, y, k, radius, giantTrunk)) {
                    mutableBlockPos.setWithOffset(centerPos, j, y, k);
                    tryPlaceLeaf(world, replacer, random, config, mutableBlockPos);
                }
            }
        }

    }

    protected static void tryPlaceLeaf(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, BlockPosition pos) {
        if (WorldGenTrees.validTreePos(world, pos)) {
            replacer.accept(pos, config.foliageProvider.getState(random, pos));
        }

    }

    public static final class FoliageAttachment {
        private final BlockPosition pos;
        private final int radiusOffset;
        private final boolean doubleTrunk;

        public FoliageAttachment(BlockPosition center, int foliageRadius, boolean giantTrunk) {
            this.pos = center;
            this.radiusOffset = foliageRadius;
            this.doubleTrunk = giantTrunk;
        }

        public BlockPosition pos() {
            return this.pos;
        }

        public int radiusOffset() {
            return this.radiusOffset;
        }

        public boolean doubleTrunk() {
            return this.doubleTrunk;
        }
    }
}
