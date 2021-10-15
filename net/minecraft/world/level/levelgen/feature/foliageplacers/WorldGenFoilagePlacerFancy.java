package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenFoilagePlacerFancy extends WorldGenFoilagePlacerBlob {
    public static final Codec<WorldGenFoilagePlacerFancy> CODEC = RecordCodecBuilder.create((instance) -> {
        return blobParts(instance).apply(instance, WorldGenFoilagePlacerFancy::new);
    });

    public WorldGenFoilagePlacerFancy(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset, height);
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.FANCY_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        for(int i = offset; i >= offset - foliageHeight; --i) {
            int j = radius + (i != offset && i != offset - foliageHeight ? 1 : 0);
            this.placeLeavesRow(world, replacer, random, config, treeNode.pos(), j, i, treeNode.doubleTrunk());
        }

    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return MathHelper.square((float)dx + 0.5F) + MathHelper.square((float)dz + 0.5F) > (float)(radius * radius);
    }
}
