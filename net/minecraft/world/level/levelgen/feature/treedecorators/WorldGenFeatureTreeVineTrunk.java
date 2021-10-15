package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.BlockVine;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class WorldGenFeatureTreeVineTrunk extends WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTreeVineTrunk> CODEC;
    public static final WorldGenFeatureTreeVineTrunk INSTANCE = new WorldGenFeatureTreeVineTrunk();

    @Override
    protected WorldGenFeatureTrees<?> type() {
        return WorldGenFeatureTrees.TRUNK_VINE;
    }

    @Override
    public void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions) {
        logPositions.forEach((pos) -> {
            if (random.nextInt(3) > 0) {
                BlockPosition blockPos = pos.west();
                if (WorldGenerator.isAir(world, blockPos)) {
                    placeVine(replacer, blockPos, BlockVine.EAST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPosition blockPos2 = pos.east();
                if (WorldGenerator.isAir(world, blockPos2)) {
                    placeVine(replacer, blockPos2, BlockVine.WEST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPosition blockPos3 = pos.north();
                if (WorldGenerator.isAir(world, blockPos3)) {
                    placeVine(replacer, blockPos3, BlockVine.SOUTH);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPosition blockPos4 = pos.south();
                if (WorldGenerator.isAir(world, blockPos4)) {
                    placeVine(replacer, blockPos4, BlockVine.NORTH);
                }
            }

        });
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
