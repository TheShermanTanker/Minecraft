package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.MathHelper;

public class NoiseSlider {
    public static final Codec<NoiseSlider> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.DOUBLE.fieldOf("target").forGetter((noiseSlider) -> {
            return noiseSlider.target;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("size").forGetter((noiseSlider) -> {
            return noiseSlider.size;
        }), Codec.INT.fieldOf("offset").forGetter((noiseSlider) -> {
            return noiseSlider.offset;
        })).apply(instance, NoiseSlider::new);
    });
    private final double target;
    private final int size;
    private final int offset;

    public NoiseSlider(double target, int size, int offset) {
        this.target = target;
        this.size = size;
        this.offset = offset;
    }

    public double applySlide(double d, int i) {
        if (this.size <= 0) {
            return d;
        } else {
            double e = (double)(i - this.offset) / (double)this.size;
            return MathHelper.clampedLerp(this.target, d, e);
        }
    }
}
