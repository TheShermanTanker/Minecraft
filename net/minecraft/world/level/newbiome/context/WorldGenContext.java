package net.minecraft.world.level.newbiome.context;

import net.minecraft.world.level.levelgen.synth.NoiseGeneratorPerlin;

public interface WorldGenContext {
    int nextRandom(int bound);

    NoiseGeneratorPerlin getBiomeNoise();
}
