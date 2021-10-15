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

public class WorldGenFoilagePlacerSpruce extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerSpruce> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(IntProvider.codec(0, 24).fieldOf("trunk_height").forGetter((placer) -> {
            return placer.trunkHeight;
        })).apply(instance, WorldGenFoilagePlacerSpruce::new);
    });
    private final IntProvider trunkHeight;

    public WorldGenFoilagePlacerSpruce(IntProvider radius, IntProvider offset, IntProvider trunkHeight) {
        super(radius, offset);
        this.trunkHeight = trunkHeight;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.SPRUCE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPosition blockPos = treeNode.pos();
        int i = random.nextInt(2);
        int j = 1;
        int k = 0;

        for(int l = offset; l >= -foliageHeight; --l) {
            this.placeLeavesRow(world, replacer, random, config, blockPos, i, l, treeNode.doubleTrunk());
            if (i >= j) {
                i = k;
                k = 1;
                j = Math.min(j + 1, radius + treeNode.radiusOffset());
            } else {
                ++i;
            }
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return Math.max(4, trunkHeight - this.trunkHeight.sample(random));
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return dx == radius && dz == radius && radius > 0;
    }
}
