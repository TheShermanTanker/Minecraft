package net.minecraft.world.level.biome;

public interface GenLayerZoomer {
    BiomeBase getBiome(long seed, int x, int y, int z, BiomeManager.Provider storage);
}
