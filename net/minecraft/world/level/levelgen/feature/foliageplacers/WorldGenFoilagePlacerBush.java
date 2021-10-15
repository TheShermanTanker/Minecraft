package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenFoilagePlacerBush extends WorldGenFoilagePlacerBlob {
    public static final Codec<WorldGenFoilagePlacerBush> CODEC = RecordCodecBuilder.create((instance) -> {
        return blobParts(instance).apply(instance, WorldGenFoilagePlacerBush::new);
    });

    public WorldGenFoilagePlacerBush(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset, height);
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.BUSH_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        for(int i = offset; i >= offset - foliageHeight; --i) {
            int j = radius + treeNode.radiusOffset() - 1 - i;
            this.placeLeavesRow(world, replacer, random, config, treeNode.pos(), j, i, treeNode.doubleTrunk());
        }

    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return dx == radius && dz == radius && random.nextInt(2) == 0;
    }
}
