package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerForking extends TrunkPlacer {
    public static final Codec<TrunkPlacerForking> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerForking::new);
    });

    public TrunkPlacerForking(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.FORKING_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        setDirtAt(world, replacer, random, startPos.below(), config);
        List<WorldGenFoilagePlacer.FoliageAttachment> list = Lists.newArrayList();
        EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
        int i = height - random.nextInt(4) - 1;
        int j = 3 - random.nextInt(3);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int k = startPos.getX();
        int l = startPos.getZ();
        OptionalInt optionalInt = OptionalInt.empty();

        for(int m = 0; m < height; ++m) {
            int n = startPos.getY() + m;
            if (m >= i && j > 0) {
                k += direction.getAdjacentX();
                l += direction.getAdjacentZ();
                --j;
            }

            if (placeLog(world, replacer, random, mutableBlockPos.set(k, n, l), config)) {
                optionalInt = OptionalInt.of(n + 1);
            }
        }

        if (optionalInt.isPresent()) {
            list.add(new WorldGenFoilagePlacer.FoliageAttachment(new BlockPosition(k, optionalInt.getAsInt(), l), 1, false));
        }

        k = startPos.getX();
        l = startPos.getZ();
        EnumDirection direction2 = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
        if (direction2 != direction) {
            int o = i - random.nextInt(2) - 1;
            int p = 1 + random.nextInt(3);
            optionalInt = OptionalInt.empty();

            for(int q = o; q < height && p > 0; --p) {
                if (q >= 1) {
                    int r = startPos.getY() + q;
                    k += direction2.getAdjacentX();
                    l += direction2.getAdjacentZ();
                    if (placeLog(world, replacer, random, mutableBlockPos.set(k, r, l), config)) {
                        optionalInt = OptionalInt.of(r + 1);
                    }
                }

                ++q;
            }

            if (optionalInt.isPresent()) {
                list.add(new WorldGenFoilagePlacer.FoliageAttachment(new BlockPosition(k, optionalInt.getAsInt(), l), 0, false));
            }
        }

        return list;
    }
}
