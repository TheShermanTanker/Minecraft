package net.minecraft.world.level.levelgen.carver;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class CarvingContext extends WorldGenerationContext {
    private final ChunkGeneratorAbstract generator;
    private final IRegistryCustom registryAccess;
    private final NoiseChunk noiseChunk;

    public CarvingContext(ChunkGeneratorAbstract chunkGenerator, IRegistryCustom registryManager, IWorldHeightAccess heightLimitView, NoiseChunk chunkNoiseSampler) {
        super(chunkGenerator, heightLimitView);
        this.generator = chunkGenerator;
        this.registryAccess = registryManager;
        this.noiseChunk = chunkNoiseSampler;
    }

    /** @deprecated */
    @Deprecated
    public Optional<IBlockData> topMaterial(Function<BlockPosition, BiomeBase> posToBiome, IChunkAccess chunk, BlockPosition pos, boolean hasFluid) {
        return this.generator.topMaterial(this, posToBiome, chunk, this.noiseChunk, pos, hasFluid);
    }

    /** @deprecated */
    @Deprecated
    public IRegistryCustom registryAccess() {
        return this.registryAccess;
    }
}
