package net.minecraft.world.level.levelgen.placement;

import java.util.BitSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WorldGenDecoratorContext extends WorldGenerationContext {
    private final GeneratorAccessSeed level;

    public WorldGenDecoratorContext(GeneratorAccessSeed world, ChunkGenerator generator) {
        super(generator, world);
        this.level = world;
    }

    public int getHeight(HeightMap.Type heightmap, int x, int z) {
        return this.level.getHeight(heightmap, x, z);
    }

    public BitSet getCarvingMask(ChunkCoordIntPair chunkPos, WorldGenStage.Features carver) {
        return ((ProtoChunk)this.level.getChunkAt(chunkPos.x, chunkPos.z)).getOrCreateCarvingMask(carver);
    }

    public IBlockData getBlockState(BlockPosition pos) {
        return this.level.getType(pos);
    }

    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    public GeneratorAccessSeed getLevel() {
        return this.level;
    }
}
