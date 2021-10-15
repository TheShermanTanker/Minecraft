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

public class WorldGenFoilagePlacerJungle extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerJungle> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(Codec.intRange(0, 16).fieldOf("height").forGetter((placer) -> {
            return placer.height;
        })).apply(instance, WorldGenFoilagePlacerJungle::new);
    });
    protected final int height;

    public WorldGenFoilagePlacerJungle(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset);
        this.height = height;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.MEGA_JUNGLE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        int i = treeNode.doubleTrunk() ? foliageHeight : 1 + random.nextInt(2);

        for(int j = offset; j >= offset - i; --j) {
            int k = radius + treeNode.radiusOffset() + 1 - j;
            this.placeLeavesRow(world, replacer, random, config, treeNode.pos(), k, j, treeNode.doubleTrunk());
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return this.height;
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        if (dx + dz >= 7) {
            return true;
        } else {
            return dx * dx + dz * dz > radius * radius;
        }
    }
}
