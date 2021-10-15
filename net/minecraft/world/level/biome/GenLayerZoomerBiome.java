package net.minecraft.world.level.biome;

import net.minecraft.core.QuartPos;

public enum GenLayerZoomerBiome implements GenLayerZoomer {
    INSTANCE;

    @Override
    public BiomeBase getBiome(long seed, int x, int y, int z, BiomeManager.Provider storage) {
        return storage.getBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }
}
