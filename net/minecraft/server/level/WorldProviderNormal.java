package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.levelgen.HeightMap;

public class WorldProviderNormal {
    @Nullable
    protected static BlockPosition getOverworldRespawnPos(WorldServer world, int x, int z, boolean validSpawnNeeded) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, world.getMinBuildHeight(), z);
        BiomeBase biome = world.getBiome(mutableBlockPos);
        boolean bl = world.getDimensionManager().hasCeiling();
        IBlockData blockState = biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial();
        if (validSpawnNeeded && !blockState.is(TagsBlock.VALID_SPAWN)) {
            return null;
        } else {
            Chunk levelChunk = world.getChunk(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z));
            int i = bl ? world.getChunkSource().getChunkGenerator().getSpawnHeight(world) : levelChunk.getHighestBlock(HeightMap.Type.MOTION_BLOCKING, x & 15, z & 15);
            if (i < world.getMinBuildHeight()) {
                return null;
            } else {
                int j = levelChunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, x & 15, z & 15);
                if (j <= i && j > levelChunk.getHighestBlock(HeightMap.Type.OCEAN_FLOOR, x & 15, z & 15)) {
                    return null;
                } else {
                    for(int k = i + 1; k >= world.getMinBuildHeight(); --k) {
                        mutableBlockPos.set(x, k, z);
                        IBlockData blockState2 = world.getType(mutableBlockPos);
                        if (!blockState2.getFluid().isEmpty()) {
                            break;
                        }

                        if (blockState2.equals(blockState)) {
                            return mutableBlockPos.above().immutableCopy();
                        }
                    }

                    return null;
                }
            }
        }
    }

    @Nullable
    public static BlockPosition getSpawnPosInChunk(WorldServer world, ChunkCoordIntPair chunkPos, boolean validSpawnNeeded) {
        for(int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); ++i) {
            for(int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); ++j) {
                BlockPosition blockPos = getOverworldRespawnPos(world, i, j, validSpawnNeeded);
                if (blockPos != null) {
                    return blockPos;
                }
            }
        }

        return null;
    }
}
