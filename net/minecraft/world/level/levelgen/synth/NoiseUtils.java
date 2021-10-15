package net.minecraft.world.level.levelgen.synth;

import net.minecraft.util.MathHelper;

public class NoiseUtils {
    public static double sampleNoiseAndMapToRange(NoiseGeneratorNormal sampler, double x, double y, double z, double start, double end) {
        double d = sampler.getValue(x, y, z);
        return MathHelper.map(d, -1.0D, 1.0D, start, end);
    }

    public static double biasTowardsExtreme(double d, double e) {
        return d + Math.sin(Math.PI * d) * e / Math.PI;
    }
}
