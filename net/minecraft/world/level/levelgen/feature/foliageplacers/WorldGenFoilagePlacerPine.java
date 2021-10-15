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

public class WorldGenFoilagePlacerPine extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerPine> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(IntProvider.codec(0, 24).fieldOf("height").forGetter((placer) -> {
            return placer.height;
        })).apply(instance, WorldGenFoilagePlacerPine::new);
    });
    private final IntProvider height;

    public WorldGenFoilagePlacerPine(IntProvider radius, IntProvider offset, IntProvider height) {
        super(radius, offset);
        this.height = height;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.PINE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        int i = 0;

        for(int j = offset; j >= offset - foliageHeight; --j) {
            this.placeLeavesRow(world, replacer, random, config, treeNode.pos(), i, j, treeNode.doubleTrunk());
            if (i >= 1 && j == offset - foliageHeight + 1) {
                --i;
            } else if (i < radius + treeNode.radiusOffset()) {
                ++i;
            }
        }

    }

    @Override
    public int foliageRadius(Random random, int baseHeight) {
        return super.foliageRadius(random, baseHeight) + random.nextInt(Math.max(baseHeight + 1, 1));
    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return this.height.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return dx == radius && dz == radius && radius > 0;
    }
}
