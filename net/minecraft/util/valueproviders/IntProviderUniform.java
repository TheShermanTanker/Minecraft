package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class IntProviderUniform extends IntProvider {
    public static final Codec<IntProviderUniform> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.minInclusive;
        }), Codec.INT.fieldOf("max_inclusive").forGetter((provider) -> {
            return provider.maxInclusive;
        })).apply(instance, IntProviderUniform::new);
    }).comapFlatMap((provider) -> {
        return provider.maxInclusive < provider.minInclusive ? DataResult.error("Max must be at least min, min_inclusive: " + provider.minInclusive + ", max_inclusive: " + provider.maxInclusive) : DataResult.success(provider);
    }, Function.identity());
    private final int minInclusive;
    private final int maxInclusive;

    private IntProviderUniform(int min, int max) {
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    public static IntProviderUniform of(int min, int max) {
        return new IntProviderUniform(min, max);
    }

    @Override
    public int sample(Random random) {
        return MathHelper.randomBetweenInclusive(random, this.minInclusive, this.maxInclusive);
    }

    @Override
    public int getMinValue() {
        return this.minInclusive;
    }

    @Override
    public int getMaxValue() {
        return this.maxInclusive;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.UNIFORM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}
