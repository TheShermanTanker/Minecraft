package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerGiant extends TrunkPlacer {
    public static final Codec<TrunkPlacerGiant> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerGiant::new);
    });

    public TrunkPlacerGiant(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.GIANT_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        BlockPosition blockPos = startPos.below();
        setDirtAt(world, replacer, random, blockPos, config);
        setDirtAt(world, replacer, random, blockPos.east(), config);
        setDirtAt(world, replacer, random, blockPos.south(), config);
        setDirtAt(world, replacer, random, blockPos.south().east(), config);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < height; ++i) {
            placeLogIfFreeWithOffset(world, replacer, random, mutableBlockPos, config, startPos, 0, i, 0);
            if (i < height - 1) {
                placeLogIfFreeWithOffset(world, replacer, random, mutableBlockPos, config, startPos, 1, i, 0);
                placeLogIfFreeWithOffset(world, replacer, random, mutableBlockPos, config, startPos, 1, i, 1);
                placeLogIfFreeWithOffset(world, replacer, random, mutableBlockPos, config, startPos, 0, i, 1);
            }
        }

        return ImmutableList.of(new WorldGenFoilagePlacer.FoliageAttachment(startPos.above(height), 0, true));
    }

    private static void placeLogIfFreeWithOffset(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition.MutableBlockPosition pos, WorldGenFeatureTreeConfiguration config, BlockPosition startPos, int x, int y, int z) {
        pos.setWithOffset(startPos, x, y, z);
        placeLogIfFree(world, replacer, random, pos, config);
    }
}
