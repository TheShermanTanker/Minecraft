package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public abstract class WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTree> CODEC = IRegistry.TREE_DECORATOR_TYPES.byNameCodec().dispatch(WorldGenFeatureTree::type, WorldGenFeatureTrees::codec);

    protected abstract WorldGenFeatureTrees<?> type();

    public abstract void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions);

    protected static void placeVine(BiConsumer<BlockPosition, IBlockData> replacer, BlockPosition pos, BlockStateBoolean facing) {
        replacer.accept(pos, Blocks.VINE.getBlockData().set(facing, Boolean.valueOf(true)));
    }
}
