package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;

public class BiomeManager {
    static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
    private final BiomeManager.Provider noiseBiomeSource;
    private final long biomeZoomSeed;
    private final GenLayerZoomer zoomer;

    public BiomeManager(BiomeManager.Provider storage, long seed, GenLayerZoomer type) {
        this.noiseBiomeSource = storage;
        this.biomeZoomSeed = seed;
        this.zoomer = type;
    }

    public static long obfuscateSeed(long seed) {
        return Hashing.sha256().hashLong(seed).asLong();
    }

    public BiomeManager withDifferentSource(WorldChunkManager source) {
        return new BiomeManager(source, this.biomeZoomSeed, this.zoomer);
    }

    public BiomeBase getBiome(BlockPosition pos) {
        return this.zoomer.getBiome(this.biomeZoomSeed, pos.getX(), pos.getY(), pos.getZ(), this.noiseBiomeSource);
    }

    public BiomeBase getNoiseBiomeAtPosition(double x, double y, double z) {
        int i = QuartPos.fromBlock(MathHelper.floor(x));
        int j = QuartPos.fromBlock(MathHelper.floor(y));
        int k = QuartPos.fromBlock(MathHelper.floor(z));
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public BiomeBase getNoiseBiomeAtPosition(BlockPosition pos) {
        int i = QuartPos.fromBlock(pos.getX());
        int j = QuartPos.fromBlock(pos.getY());
        int k = QuartPos.fromBlock(pos.getZ());
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public BiomeBase getNoiseBiomeAtQuart(int biomeX, int biomeY, int biomeZ) {
        return this.noiseBiomeSource.getBiome(biomeX, biomeY, biomeZ);
    }

    public BiomeBase getPrimaryBiomeAtChunk(ChunkCoordIntPair chunkPos) {
        return this.noiseBiomeSource.getPrimaryBiome(chunkPos);
    }

    public interface Provider {
        BiomeBase getBiome(int biomeX, int biomeY, int biomeZ);

        default BiomeBase getPrimaryBiome(ChunkCoordIntPair chunkPos) {
            return this.getBiome(QuartPos.fromSection(chunkPos.x) + BiomeManager.CHUNK_CENTER_QUART, 0, QuartPos.fromSection(chunkPos.z) + BiomeManager.CHUNK_CENTER_QUART);
        }
    }
}
