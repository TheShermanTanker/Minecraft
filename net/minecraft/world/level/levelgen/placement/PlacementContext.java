package net.minecraft.world.level.levelgen.placement;

import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class PlacementContext extends WorldGenerationContext {
    private final GeneratorAccessSeed level;
    private final ChunkGenerator generator;
    private final Optional<PlacedFeature> topFeature;

    public PlacementContext(GeneratorAccessSeed world, ChunkGenerator generator, Optional<PlacedFeature> placedFeature) {
        super(generator, world);
        this.level = world;
        this.generator = generator;
        this.topFeature = placedFeature;
    }

    public int getHeight(HeightMap.Type heightmap, int x, int z) {
        return this.level.getHeight(heightmap, x, z);
    }

    public CarvingMask getCarvingMask(ChunkCoordIntPair chunkPos, WorldGenStage.Features carver) {
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

    public Optional<PlacedFeature> topFeature() {
        return this.topFeature;
    }

    public ChunkGenerator generator() {
        return this.generator;
    }
}
