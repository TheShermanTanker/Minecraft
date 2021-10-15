package net.minecraft.world.level.levelgen.synth;

public interface NoiseGenerator {
    double getSurfaceNoiseValue(double x, double y, double yScale, double yMax);
}
