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

public class WorldGenFoilagePlacerAcacia extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerAcacia> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).apply(instance, WorldGenFoilagePlacerAcacia::new);
    });

    public WorldGenFoilagePlacerAcacia(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.ACACIA_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        boolean bl = treeNode.doubleTrunk();
        BlockPosition blockPos = treeNode.pos().above(offset);
        this.placeLeavesRow(world, replacer, random, config, blockPos, radius + treeNode.radiusOffset(), -1 - foliageHeight, bl);
        this.placeLeavesRow(world, replacer, random, config, blockPos, radius - 1, -foliageHeight, bl);
        this.placeLeavesRow(world, replacer, random, config, blockPos, radius + treeNode.radiusOffset() - 1, 0, bl);
    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return 0;
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        if (y == 0) {
            return (dx > 1 || dz > 1) && dx != 0 && dz != 0;
        } else {
            return dx == radius && dz == radius && radius > 0;
        }
    }
}
