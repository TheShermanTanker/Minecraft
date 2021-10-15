package net.minecraft.world.level.levelgen;

@FunctionalInterface
public interface NoiseModifier {
    NoiseModifier PASSTHROUGH = (d, i, j, k) -> {
        return d;
    };

    double modifyNoise(double weight, int x, int y, int z);
}
