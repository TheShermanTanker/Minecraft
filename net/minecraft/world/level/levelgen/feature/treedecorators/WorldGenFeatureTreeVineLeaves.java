package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.BlockVine;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class WorldGenFeatureTreeVineLeaves extends WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTreeVineLeaves> CODEC;
    public static final WorldGenFeatureTreeVineLeaves INSTANCE = new WorldGenFeatureTreeVineLeaves();

    @Override
    protected WorldGenFeatureTrees<?> type() {
        return WorldGenFeatureTrees.LEAVE_VINE;
    }

    @Override
    public void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions) {
        leavesPositions.forEach((pos) -> {
            if (random.nextInt(4) == 0) {
                BlockPosition blockPos = pos.west();
                if (WorldGenerator.isAir(world, blockPos)) {
                    addHangingVine(world, blockPos, BlockVine.EAST, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPosition blockPos2 = pos.east();
                if (WorldGenerator.isAir(world, blockPos2)) {
                    addHangingVine(world, blockPos2, BlockVine.WEST, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPosition blockPos3 = pos.north();
                if (WorldGenerator.isAir(world, blockPos3)) {
                    addHangingVine(world, blockPos3, BlockVine.SOUTH, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPosition blockPos4 = pos.south();
                if (WorldGenerator.isAir(world, blockPos4)) {
                    addHangingVine(world, blockPos4, BlockVine.NORTH, replacer);
                }
            }

        });
    }

    private static void addHangingVine(VirtualWorldReadable world, BlockPosition pos, BlockStateBoolean facing, BiConsumer<BlockPosition, IBlockData> replacer) {
        placeVine(replacer, pos, facing);
        int i = 4;

        for(BlockPosition var5 = pos.below(); WorldGenerator.isAir(world, var5) && i > 0; --i) {
            placeVine(replacer, var5, facing);
            var5 = var5.below();
        }

    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
