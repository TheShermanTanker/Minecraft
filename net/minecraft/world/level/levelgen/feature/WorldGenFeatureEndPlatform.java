package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureEndPlatform extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final BlockPosition PLATFORM_OFFSET = new BlockPosition(8, 3, 8);
    private static final ChunkCoordIntPair PLATFORM_ORIGIN_CHUNK = new ChunkCoordIntPair(PLATFORM_OFFSET);
    private static final int PLATFORM_RADIUS = 16;
    private static final int PLATFORM_RADIUS_CHUNKS = 1;

    public WorldGenFeatureEndPlatform(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    private static int checkerboardDistance(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(context.origin());
        if (checkerboardDistance(chunkPos.x, chunkPos.z, PLATFORM_ORIGIN_CHUNK.x, PLATFORM_ORIGIN_CHUNK.z) > 1) {
            return true;
        } else {
            BlockPosition blockPos = PLATFORM_OFFSET.atY(context.origin().getY() + PLATFORM_OFFSET.getY());
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int i = chunkPos.getMinBlockZ(); i <= chunkPos.getMaxBlockZ(); ++i) {
                for(int j = chunkPos.getMinBlockX(); j <= chunkPos.getMaxBlockX(); ++j) {
                    if (checkerboardDistance(blockPos.getX(), blockPos.getZ(), j, i) <= 16) {
                        mutableBlockPos.set(j, blockPos.getY(), i);
                        if (mutableBlockPos.equals(blockPos)) {
                            worldGenLevel.setTypeAndData(mutableBlockPos, Blocks.COBBLESTONE.getBlockData(), 2);
                        } else {
                            worldGenLevel.setTypeAndData(mutableBlockPos, Blocks.STONE.getBlockData(), 2);
                        }
                    }
                }
            }

            return true;
        }
    }
}
