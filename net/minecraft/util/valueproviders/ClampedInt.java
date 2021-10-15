package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class ClampedInt extends IntProvider {
    public static final Codec<ClampedInt> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IntProvider.CODEC.fieldOf("source").forGetter((clampedInt) -> {
            return clampedInt.source;
        }), Codec.INT.fieldOf("min_inclusive").forGetter((clampedInt) -> {
            return clampedInt.minInclusive;
        }), Codec.INT.fieldOf("max_inclusive").forGetter((clampedInt) -> {
            return clampedInt.maxInclusive;
        })).apply(instance, ClampedInt::new);
    }).comapFlatMap((clampedInt) -> {
        return clampedInt.maxInclusive < clampedInt.minInclusive ? DataResult.error("Max must be at least min, min_inclusive: " + clampedInt.minInclusive + ", max_inclusive: " + clampedInt.maxInclusive) : DataResult.success(clampedInt);
    }, Function.identity());
    private final IntProvider source;
    private int minInclusive;
    private int maxInclusive;

    public static ClampedInt of(IntProvider source, int min, int max) {
        return new ClampedInt(source, min, max);
    }

    public ClampedInt(IntProvider source, int min, int max) {
        this.source = source;
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    @Override
    public int sample(Random random) {
        return MathHelper.clamp(this.source.sample(random), this.minInclusive, this.maxInclusive);
    }

    @Override
    public int getMinValue() {
        return Math.max(this.minInclusive, this.source.getMinValue());
    }

    @Override
    public int getMaxValue() {
        return Math.min(this.maxInclusive, this.source.getMaxValue());
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CLAMPED;
    }
}
