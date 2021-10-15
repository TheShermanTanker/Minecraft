package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class UniformFloat extends FloatProvider {
    public static final Codec<UniformFloat> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_inclusive").forGetter((uniformFloat) -> {
            return uniformFloat.minInclusive;
        }), Codec.FLOAT.fieldOf("max_exclusive").forGetter((uniformFloat) -> {
            return uniformFloat.maxExclusive;
        })).apply(instance, UniformFloat::new);
    }).comapFlatMap((uniformFloat) -> {
        return uniformFloat.maxExclusive <= uniformFloat.minInclusive ? DataResult.error("Max must be larger than min, min_inclusive: " + uniformFloat.minInclusive + ", max_exclusive: " + uniformFloat.maxExclusive) : DataResult.success(uniformFloat);
    }, Function.identity());
    private final float minInclusive;
    private final float maxExclusive;

    private UniformFloat(float min, float max) {
        this.minInclusive = min;
        this.maxExclusive = max;
    }

    public static UniformFloat of(float min, float max) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must exceed min");
        } else {
            return new UniformFloat(min, max);
        }
    }

    @Override
    public float sample(Random random) {
        return MathHelper.randomBetween(random, this.minInclusive, this.maxExclusive);
    }

    @Override
    public float getMinValue() {
        return this.minInclusive;
    }

    @Override
    public float getMaxValue() {
        return this.maxExclusive;
    }

    @Override
    public FloatProviderType<?> getType() {
        return FloatProviderType.UNIFORM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxExclusive + "]";
    }
}
