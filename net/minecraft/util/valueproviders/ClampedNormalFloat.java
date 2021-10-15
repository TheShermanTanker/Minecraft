package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.MathHelper;

public class ClampedNormalFloat extends FloatProvider {
    public static final Codec<ClampedNormalFloat> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("mean").forGetter((clampedNormalFloat) -> {
            return clampedNormalFloat.mean;
        }), Codec.FLOAT.fieldOf("deviation").forGetter((clampedNormalFloat) -> {
            return clampedNormalFloat.deviation;
        }), Codec.FLOAT.fieldOf("min").forGetter((clampedNormalFloat) -> {
            return clampedNormalFloat.min;
        }), Codec.FLOAT.fieldOf("max").forGetter((clampedNormalFloat) -> {
            return clampedNormalFloat.max;
        })).apply(instance, ClampedNormalFloat::new);
    }).comapFlatMap((clampedNormalFloat) -> {
        return clampedNormalFloat.max < clampedNormalFloat.min ? DataResult.error("Max must be larger than min: [" + clampedNormalFloat.min + ", " + clampedNormalFloat.max + "]") : DataResult.success(clampedNormalFloat);
    }, Function.identity());
    private float mean;
    private float deviation;
    private float min;
    private float max;

    public static ClampedNormalFloat of(float mean, float deviation, float min, float max) {
        return new ClampedNormalFloat(mean, deviation, min, max);
    }

    private ClampedNormalFloat(float mean, float deviation, float min, float max) {
        this.mean = mean;
        this.deviation = deviation;
        this.min = min;
        this.max = max;
    }

    @Override
    public float sample(Random random) {
        return sample(random, this.mean, this.deviation, this.min, this.max);
    }

    public static float sample(Random random, float mean, float deviation, float min, float max) {
        return MathHelper.clamp(MathHelper.normal(random, mean, deviation), min, max);
    }

    @Override
    public float getMinValue() {
        return this.min;
    }

    @Override
    public float getMaxValue() {
        return this.max;
    }

    @Override
    public FloatProviderType<?> getType() {
        return FloatProviderType.CLAMPED_NORMAL;
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min + "-" + this.max + "]";
    }
}
