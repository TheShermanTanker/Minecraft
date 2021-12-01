package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public class NoiseThresholdProvider extends NoiseBasedStateProvider {
    public static final Codec<NoiseThresholdProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return noiseCodec(instance).and(instance.group(Codec.floatRange(-1.0F, 1.0F).fieldOf("threshold").forGetter((noiseThresholdProvider) -> {
            return noiseThresholdProvider.threshold;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("high_chance").forGetter((noiseThresholdProvider) -> {
            return noiseThresholdProvider.highChance;
        }), IBlockData.CODEC.fieldOf("default_state").forGetter((noiseThresholdProvider) -> {
            return noiseThresholdProvider.defaultState;
        }), Codec.list(IBlockData.CODEC).fieldOf("low_states").forGetter((noiseThresholdProvider) -> {
            return noiseThresholdProvider.lowStates;
        }), Codec.list(IBlockData.CODEC).fieldOf("high_states").forGetter((noiseThresholdProvider) -> {
            return noiseThresholdProvider.highStates;
        }))).apply(instance, NoiseThresholdProvider::new);
    });
    private final float threshold;
    private final float highChance;
    private final IBlockData defaultState;
    private final List<IBlockData> lowStates;
    private final List<IBlockData> highStates;

    public NoiseThresholdProvider(long seed, NormalNoise$NoiseParameters noiseParameters, float scale, float threshold, float highChance, IBlockData defaultState, List<IBlockData> lowStates, List<IBlockData> highStates) {
        super(seed, noiseParameters, scale);
        this.threshold = threshold;
        this.highChance = highChance;
        this.defaultState = defaultState;
        this.lowStates = lowStates;
        this.highStates = highStates;
    }

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.NOISE_THRESHOLD_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        double d = this.getNoiseValue(pos, (double)this.scale);
        if (d < (double)this.threshold) {
            return SystemUtils.getRandom(this.lowStates, random);
        } else {
            return random.nextFloat() < this.highChance ? SystemUtils.getRandom(this.highStates, random) : this.defaultState;
        }
    }
}
