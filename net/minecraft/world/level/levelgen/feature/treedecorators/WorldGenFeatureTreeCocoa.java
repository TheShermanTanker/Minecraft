package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.BlockCocoa;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class WorldGenFeatureTreeCocoa extends WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTreeCocoa> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(WorldGenFeatureTreeCocoa::new, (decorator) -> {
        return decorator.probability;
    }).codec();
    private final float probability;

    public WorldGenFeatureTreeCocoa(float probability) {
        this.probability = probability;
    }

    @Override
    protected WorldGenFeatureTrees<?> type() {
        return WorldGenFeatureTrees.COCOA;
    }

    @Override
    public void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions) {
        if (!(random.nextFloat() >= this.probability)) {
            int i = logPositions.get(0).getY();
            logPositions.stream().filter((pos) -> {
                return pos.getY() - i <= 2;
            }).forEach((pos) -> {
                for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                    if (random.nextFloat() <= 0.25F) {
                        EnumDirection direction2 = direction.opposite();
                        BlockPosition blockPos = pos.offset(direction2.getAdjacentX(), 0, direction2.getAdjacentZ());
                        if (WorldGenerator.isAir(world, blockPos)) {
                            replacer.accept(blockPos, Blocks.COCOA.getBlockData().set(BlockCocoa.AGE, Integer.valueOf(random.nextInt(3))).set(BlockCocoa.FACING, direction));
                        }
                    }
                }

            });
        }
    }
}
