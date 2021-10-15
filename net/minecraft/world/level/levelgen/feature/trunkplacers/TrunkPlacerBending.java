package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenTrees;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerBending extends TrunkPlacer {
    public static final Codec<TrunkPlacerBending> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).and(instance.group(ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter((placer) -> {
            return placer.minHeightForLeaves;
        }), IntProvider.codec(1, 64).fieldOf("bend_length").forGetter((placer) -> {
            return placer.bendLength;
        }))).apply(instance, TrunkPlacerBending::new);
    });
    private final int minHeightForLeaves;
    private final IntProvider bendLength;

    public TrunkPlacerBending(int baseHeight, int firstRandomHeight, int secondRandomHeight, int minHeightForLeaves, IntProvider bendLength) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
        this.minHeightForLeaves = minHeightForLeaves;
        this.bendLength = bendLength;
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.BENDING_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
        int i = height - 1;
        BlockPosition.MutableBlockPosition mutableBlockPos = startPos.mutable();
        BlockPosition blockPos = mutableBlockPos.below();
        setDirtAt(world, replacer, random, blockPos, config);
        List<WorldGenFoilagePlacer.FoliageAttachment> list = Lists.newArrayList();

        for(int j = 0; j <= i; ++j) {
            if (j + 1 >= i + random.nextInt(2)) {
                mutableBlockPos.move(direction);
            }

            if (WorldGenTrees.validTreePos(world, mutableBlockPos)) {
                placeLog(world, replacer, random, mutableBlockPos, config);
            }

            if (j >= this.minHeightForLeaves) {
                list.add(new WorldGenFoilagePlacer.FoliageAttachment(mutableBlockPos.immutableCopy(), 0, false));
            }

            mutableBlockPos.move(EnumDirection.UP);
        }

        int k = this.bendLength.sample(random);

        for(int l = 0; l <= k; ++l) {
            if (WorldGenTrees.validTreePos(world, mutableBlockPos)) {
                placeLog(world, replacer, random, mutableBlockPos, config);
            }

            list.add(new WorldGenFoilagePlacer.FoliageAttachment(mutableBlockPos.immutableCopy(), 0, false));
            mutableBlockPos.move(direction);
        }

        return list;
    }
}
