package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorNoiseConfiguration;

public class WorldGenDecoratorCountNoise extends RepeatingDecorator<WorldGenFeatureDecoratorNoiseConfiguration> {
    public WorldGenDecoratorCountNoise(Codec<WorldGenFeatureDecoratorNoiseConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenFeatureDecoratorNoiseConfiguration config, BlockPosition pos) {
        double d = BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() / 200.0D, (double)pos.getZ() / 200.0D, false);
        return d < config.noiseLevel ? config.belowNoise : config.aboveNoise;
    }
}
