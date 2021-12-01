package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.levelgen.HeightMap;

public class WorldProviderNormal {
    @Nullable
    protected static BlockPosition getOverworldRespawnPos(WorldServer world, int x, int z) {
        boolean bl = world.getDimensionManager().hasCeiling();
        Chunk levelChunk = world.getChunk(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z));
        int i = bl ? world.getChunkSource().getChunkGenerator().getSpawnHeight(world) : levelChunk.getHighestBlock(HeightMap.Type.MOTION_BLOCKING, x & 15, z & 15);
        if (i < world.getMinBuildHeight()) {
            return null;
        } else {
            int j = levelChunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, x & 15, z & 15);
            if (j <= i && j > levelChunk.getHighestBlock(HeightMap.Type.OCEAN_FLOOR, x & 15, z & 15)) {
                return null;
            } else {
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

                for(int k = i + 1; k >= world.getMinBuildHeight(); --k) {
                    mutableBlockPos.set(x, k, z);
                    IBlockData blockState = world.getType(mutableBlockPos);
                    if (!blockState.getFluid().isEmpty()) {
                        break;
                    }

                    if (Block.isFaceFull(blockState.getCollisionShape(world, mutableBlockPos), EnumDirection.UP)) {
                        return mutableBlockPos.above().immutableCopy();
                    }
                }

                return null;
            }
        }
    }

    @Nullable
    public static BlockPosition getSpawnPosInChunk(WorldServer world, ChunkCoordIntPair chunkPos) {
        if (SharedConstants.debugVoidTerrain(chunkPos)) {
            return null;
        } else {
            for(int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); ++i) {
                for(int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); ++j) {
                    BlockPosition blockPos = getOverworldRespawnPos(world, i, j);
                    if (blockPos != null) {
                        return blockPos;
                    }
                }
            }

            return null;
        }
    }
}
