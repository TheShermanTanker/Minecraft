package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public class NoiseProvider extends NoiseBasedStateProvider {
    public static final Codec<NoiseProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return noiseProviderCodec(instance).apply(instance, NoiseProvider::new);
    });
    protected final List<IBlockData> states;

    protected static <P extends NoiseProvider> P4<Mu<P>, Long, NormalNoise$NoiseParameters, Float, List<IBlockData>> noiseProviderCodec(Instance<P> instance) {
        return noiseCodec(instance).and(Codec.list(IBlockData.CODEC).fieldOf("states").forGetter((noiseProvider) -> {
            return noiseProvider.states;
        }));
    }

    public NoiseProvider(long seed, NormalNoise$NoiseParameters noiseParameters, float scale, List<IBlockData> states) {
        super(seed, noiseParameters, scale);
        this.states = states;
    }

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.NOISE_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        return this.getRandomState(this.states, pos, (double)this.scale);
    }

    protected IBlockData getRandomState(List<IBlockData> states, BlockPosition pos, double scale) {
        double d = this.getNoiseValue(pos, scale);
        return this.getRandomState(states, d);
    }

    protected IBlockData getRandomState(List<IBlockData> states, double value) {
        double d = MathHelper.clamp((1.0D + value) / 2.0D, 0.0D, 0.9999D);
        return states.get((int)(d * (double)states.size()));
    }
}
