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

public class TrunkPlacerStraight extends TrunkPlacer {
    public static final Codec<TrunkPlacerStraight> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerStraight::new);
    });

    public TrunkPlacerStraight(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.STRAIGHT_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        setDirtAt(world, replacer, random, startPos.below(), config);

        for(int i = 0; i < height; ++i) {
            placeLog(world, replacer, random, startPos.above(i), config);
        }

        return ImmutableList.of(new WorldGenFoilagePlacer.FoliageAttachment(startPos.above(height), 0, false));
    }
}
