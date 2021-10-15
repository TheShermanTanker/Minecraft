package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenFoilagePlacerBlob extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerBlob> CODEC = RecordCodecBuilder.create((instance) -> {
        return blobParts(instance).apply(instance, WorldGenFoilagePlacerBlob::new);
    });
    protected final int height;

    protected static <P extends WorldGenFoilagePlacerBlob> P3<Mu<P>, IntProvider, IntProvider, Integer> blobParts(Instance<P> builder) {
        return foliagePlacerParts(builder).and(Codec.intRange(0, 16).fieldOf("height").forGetter((placer) -> {
            return placer.height;
        }));
    }

    public WorldGenFoilagePlacerBlob(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset);
        this.height = height;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.BLOB_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        for(int i = offset; i >= offset - foliageHeight; --i) {
            int j = Math.max(radius + treeNode.radiusOffset() - 1 - i / 2, 0);
            this.placeLeavesRow(world, replacer, random, config, treeNode.pos(), j, i, treeNode.doubleTrunk());
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return this.height;
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return dx == radius && dz == radius && (random.nextInt(2) == 0 || y == 0);
    }
}
