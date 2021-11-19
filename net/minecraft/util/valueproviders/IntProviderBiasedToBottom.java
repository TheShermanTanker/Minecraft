package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;

public class IntProviderBiasedToBottom extends IntProvider {
    public static final Codec<IntProviderBiasedToBottom> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("min_inclusive").forGetter((biasedToBottomInt) -> {
            return biasedToBottomInt.minInclusive;
        }), Codec.INT.fieldOf("max_inclusive").forGetter((biasedToBottomInt) -> {
            return biasedToBottomInt.maxInclusive;
        })).apply(instance, IntProviderBiasedToBottom::new);
    }).comapFlatMap((biasedToBottomInt) -> {
        return biasedToBottomInt.maxInclusive < biasedToBottomInt.minInclusive ? DataResult.error("Max must be at least min, min_inclusive: " + biasedToBottomInt.minInclusive + ", max_inclusive: " + biasedToBottomInt.maxInclusive) : DataResult.success(biasedToBottomInt);
    }, Function.identity());
    private final int minInclusive;
    private final int maxInclusive;

    private IntProviderBiasedToBottom(int min, int max) {
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    public static IntProviderBiasedToBottom of(int min, int max) {
        return new IntProviderBiasedToBottom(min, max);
    }

    @Override
    public int sample(Random random) {
        return this.minInclusive + random.nextInt(random.nextInt(this.maxInclusive - this.minInclusive + 1) + 1);
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
        return IntProviderType.BIASED_TO_BOTTOM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}
