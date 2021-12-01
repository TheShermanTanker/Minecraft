package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;

public abstract class WorldGenMegaTreeProvider extends WorldGenTreeProvider {
    @Override
    public boolean growTree(WorldServer world, ChunkGenerator chunkGenerator, BlockPosition pos, IBlockData state, Random random) {
        for(int i = 0; i >= -1; --i) {
            for(int j = 0; j >= -1; --j) {
                if (isTwoByTwoSapling(state, world, pos, i, j)) {
                    return this.placeMega(world, chunkGenerator, pos, state, random, i, j);
                }
            }
        }

        return super.growTree(world, chunkGenerator, pos, state, random);
    }

    @Nullable
    protected abstract WorldGenFeatureConfigured<?, ?> getConfiguredMegaFeature(Random random);

    public boolean placeMega(WorldServer world, ChunkGenerator chunkGenerator, BlockPosition pos, IBlockData state, Random random, int x, int z) {
        WorldGenFeatureConfigured<?, ?> configuredFeature = this.getConfiguredMegaFeature(random);
        if (configuredFeature == null) {
            return false;
        } else {
            IBlockData blockState = Blocks.AIR.getBlockData();
            world.setTypeAndData(pos.offset(x, 0, z), blockState, 4);
            world.setTypeAndData(pos.offset(x + 1, 0, z), blockState, 4);
            world.setTypeAndData(pos.offset(x, 0, z + 1), blockState, 4);
            world.setTypeAndData(pos.offset(x + 1, 0, z + 1), blockState, 4);
            if (configuredFeature.place(world, chunkGenerator, random, pos.offset(x, 0, z))) {
                return true;
            } else {
                world.setTypeAndData(pos.offset(x, 0, z), state, 4);
                world.setTypeAndData(pos.offset(x + 1, 0, z), state, 4);
                world.setTypeAndData(pos.offset(x, 0, z + 1), state, 4);
                world.setTypeAndData(pos.offset(x + 1, 0, z + 1), state, 4);
                return false;
            }
        }
    }

    public static boolean isTwoByTwoSapling(IBlockData state, IBlockAccess world, BlockPosition pos, int x, int z) {
        Block block = state.getBlock();
        return world.getType(pos.offset(x, 0, z)).is(block) && world.getType(pos.offset(x + 1, 0, z)).is(block) && world.getType(pos.offset(x, 0, z + 1)).is(block) && world.getType(pos.offset(x + 1, 0, z + 1)).is(block);
    }
}
