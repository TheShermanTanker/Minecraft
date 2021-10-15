package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenTrees;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerDarkOak extends TrunkPlacer {
    public static final Codec<TrunkPlacerDarkOak> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerDarkOak::new);
    });

    public TrunkPlacerDarkOak(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.DARK_OAK_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        List<WorldGenFoilagePlacer.FoliageAttachment> list = Lists.newArrayList();
        BlockPosition blockPos = startPos.below();
        setDirtAt(world, replacer, random, blockPos, config);
        setDirtAt(world, replacer, random, blockPos.east(), config);
        setDirtAt(world, replacer, random, blockPos.south(), config);
        setDirtAt(world, replacer, random, blockPos.south().east(), config);
        EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
        int i = height - random.nextInt(4);
        int j = 2 - random.nextInt(3);
        int k = startPos.getX();
        int l = startPos.getY();
        int m = startPos.getZ();
        int n = k;
        int o = m;
        int p = l + height - 1;

        for(int q = 0; q < height; ++q) {
            if (q >= i && j > 0) {
                n += direction.getAdjacentX();
                o += direction.getAdjacentZ();
                --j;
            }

            int r = l + q;
            BlockPosition blockPos2 = new BlockPosition(n, r, o);
            if (WorldGenTrees.isAirOrLeaves(world, blockPos2)) {
                placeLog(world, replacer, random, blockPos2, config);
                placeLog(world, replacer, random, blockPos2.east(), config);
                placeLog(world, replacer, random, blockPos2.south(), config);
                placeLog(world, replacer, random, blockPos2.east().south(), config);
            }
        }

        list.add(new WorldGenFoilagePlacer.FoliageAttachment(new BlockPosition(n, p, o), 0, true));

        for(int s = -1; s <= 2; ++s) {
            for(int t = -1; t <= 2; ++t) {
                if ((s < 0 || s > 1 || t < 0 || t > 1) && random.nextInt(3) <= 0) {
                    int u = random.nextInt(3) + 2;

                    for(int v = 0; v < u; ++v) {
                        placeLog(world, replacer, random, new BlockPosition(k + s, p - v - 1, m + t), config);
                    }

                    list.add(new WorldGenFoilagePlacer.FoliageAttachment(new BlockPosition(n + s, p, o + t), 0, false));
                }
            }
        }

        return list;
    }
}
