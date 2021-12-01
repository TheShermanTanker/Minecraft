package net.minecraft.world.level.biome;

public interface BiomeResolver {
    BiomeBase getNoiseBiome(int x, int y, int z, Climate.Sampler noise);
}
