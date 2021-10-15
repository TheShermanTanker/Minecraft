package net.minecraft.world.level;

import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.world.level.biome.BiomeBase;

public interface ColorResolver {
    @DontObfuscate
    int getColor(BiomeBase biome, double x, double z);
}
