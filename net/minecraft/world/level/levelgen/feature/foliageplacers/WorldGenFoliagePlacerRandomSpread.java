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

public class WorldGenFoliagePlacerRandomSpread extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoliagePlacerRandomSpread> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(instance.group(IntProvider.codec(1, 512).fieldOf("foliage_height").forGetter((placer) -> {
            return placer.foliageHeight;
        }), Codec.intRange(0, 256).fieldOf("leaf_placement_attempts").forGetter((placer) -> {
            return placer.leafPlacementAttempts;
        }))).apply(instance, WorldGenFoliagePlacerRandomSpread::new);
    });
    private final IntProvider foliageHeight;
    private final int leafPlacementAttempts;

    public WorldGenFoliagePlacerRandomSpread(IntProvider radius, IntProvider offset, IntProvider foliageHeight, int leafPlacementAttempts) {
        super(radius, offset);
        this.foliageHeight = foliageHeight;
        this.leafPlacementAttempts = leafPlacementAttempts;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.RANDOM_SPREAD_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPosition blockPos = treeNode.pos();
        BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();

        for(int i = 0; i < this.leafPlacementAttempts; ++i) {
            mutableBlockPos.setWithOffset(blockPos, random.nextInt(radius) - random.nextInt(radius), random.nextInt(foliageHeight) - random.nextInt(foliageHeight), random.nextInt(radius) - random.nextInt(radius));
            tryPlaceLeaf(world, replacer, random, config, mutableBlockPos);
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return this.foliageHeight.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return false;
    }
}
