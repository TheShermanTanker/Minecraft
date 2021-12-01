package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public abstract class WorldGenTreeProvider {
    @Nullable
    protected abstract WorldGenFeatureConfigured<?, ?> getConfiguredFeature(Random random, boolean bees);

    public boolean growTree(WorldServer world, ChunkGenerator chunkGenerator, BlockPosition pos, IBlockData state, Random random) {
        WorldGenFeatureConfigured<?, ?> configuredFeature = this.getConfiguredFeature(random, this.hasFlowers(world, pos));
        if (configuredFeature == null) {
            return false;
        } else {
            world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 4);
            if (configuredFeature.place(world, chunkGenerator, random, pos)) {
                return true;
            } else {
                world.setTypeAndData(pos, state, 4);
                return false;
            }
        }
    }

    private boolean hasFlowers(GeneratorAccess world, BlockPosition pos) {
        for(BlockPosition blockPos : BlockPosition.MutableBlockPosition.betweenClosed(pos.below().north(2).west(2), pos.above().south(2).east(2))) {
            if (world.getType(blockPos).is(TagsBlock.FLOWERS)) {
                return true;
            }
        }

        return false;
    }
}
