package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;

public class WorldGenDecoratorCountNoiseBiased extends RepeatingDecorator<WorldGenDecoratorNoiseConfiguration> {
    public WorldGenDecoratorCountNoiseBiased(Codec<WorldGenDecoratorNoiseConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenDecoratorNoiseConfiguration config, BlockPosition pos) {
        double d = BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() / config.noiseFactor, (double)pos.getZ() / config.noiseFactor, false);
        return (int)Math.ceil((d + config.noiseOffset) * (double)config.noiseToCountRatio);
    }
}
