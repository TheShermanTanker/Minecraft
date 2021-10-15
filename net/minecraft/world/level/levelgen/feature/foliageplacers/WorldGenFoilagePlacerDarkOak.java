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

public class WorldGenFoilagePlacerDarkOak extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerDarkOak> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).apply(instance, WorldGenFoilagePlacerDarkOak::new);
    });

    public WorldGenFoilagePlacerDarkOak(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.DARK_OAK_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPosition blockPos = treeNode.pos().above(offset);
        boolean bl = treeNode.doubleTrunk();
        if (bl) {
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, -1, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 3, 0, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, 1, bl);
            if (random.nextBoolean()) {
                this.placeLeavesRow(world, replacer, random, config, blockPos, radius, 2, bl);
            }
        } else {
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, -1, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 1, 0, bl);
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return 4;
    }

    @Override
    protected boolean shouldSkipLocationSigned(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return y != 0 || !giantTrunk || dx != -radius && dx < radius || dz != -radius && dz < radius ? super.shouldSkipLocationSigned(random, dx, y, dz, radius, giantTrunk) : true;
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        if (y == -1 && !giantTrunk) {
            return dx == radius && dz == radius;
        } else if (y == 1) {
            return dx + dz > radius * 2 - 2;
        } else {
            return false;
        }
    }
}
