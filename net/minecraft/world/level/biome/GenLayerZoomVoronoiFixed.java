package net.minecraft.world.level.biome;

public enum GenLayerZoomVoronoiFixed implements GenLayerZoomer {
    INSTANCE;

    @Override
    public BiomeBase getBiome(long seed, int x, int y, int z, BiomeManager.Provider storage) {
        return GenLayerZoomVoronoi.INSTANCE.getBiome(seed, x, 0, z, storage);
    }
}
