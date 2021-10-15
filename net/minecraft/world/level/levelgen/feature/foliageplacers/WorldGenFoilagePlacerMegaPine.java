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

public class WorldGenFoilagePlacerMegaPine extends WorldGenFoilagePlacer {
    public static final Codec<WorldGenFoilagePlacerMegaPine> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(IntProvider.codec(0, 24).fieldOf("crown_height").forGetter((placer) -> {
            return placer.crownHeight;
        })).apply(instance, WorldGenFoilagePlacerMegaPine::new);
    });
    private final IntProvider crownHeight;

    public WorldGenFoilagePlacerMegaPine(IntProvider radius, IntProvider offset, IntProvider crownHeight) {
        super(radius, offset);
        this.crownHeight = crownHeight;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.MEGA_PINE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, WorldGenFeatureTreeConfiguration config, int trunkHeight, WorldGenFoilagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPosition blockPos = treeNode.pos();
        int i = 0;

        for(int j = blockPos.getY() - foliageHeight + offset; j <= blockPos.getY() + offset; ++j) {
            int k = blockPos.getY() - j;
            int l = radius + treeNode.radiusOffset() + MathHelper.floor((float)k / (float)foliageHeight * 3.5F);
            int m;
            if (k > 0 && l == i && (j & 1) == 0) {
                m = l + 1;
            } else {
                m = l;
            }

            this.placeLeavesRow(world, replacer, random, config, new BlockPosition(blockPos.getX(), j, blockPos.getZ()), m, 0, treeNode.doubleTrunk());
            i = l;
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, WorldGenFeatureTreeConfiguration config) {
        return this.crownHeight.sample(random);
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
