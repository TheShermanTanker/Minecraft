package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.levelgen.synth.NoiseGeneratorPerlin;
import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer1;

public enum GenLayerOceanEdge implements AreaTransformer1 {
    INSTANCE;

    @Override
    public int applyPixel(WorldGenContext context, int x, int y) {
        NoiseGeneratorPerlin improvedNoise = context.getBiomeNoise();
        double d = improvedNoise.noise((double)x / 8.0D, (double)y / 8.0D, 0.0D);
        if (d > 0.4D) {
            return 44;
        } else if (d > 0.2D) {
            return 45;
        } else if (d < -0.4D) {
            return 10;
        } else {
            return d < -0.2D ? 46 : 0;
        }
    }
}
