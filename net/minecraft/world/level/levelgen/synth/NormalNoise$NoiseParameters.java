package net.minecraft.world.level.levelgen.synth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.RegistryFileCodec;

public class NormalNoise$NoiseParameters {
    private final int firstOctave;
    private final DoubleList amplitudes;
    public static final Codec<NormalNoise$NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise$NoiseParameters::firstOctave), Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise$NoiseParameters::amplitudes)).apply(instance, NormalNoise$NoiseParameters::new);
    });
    public static final Codec<Supplier<NormalNoise$NoiseParameters>> CODEC = RegistryFileCodec.create(IRegistry.NOISE_REGISTRY, DIRECT_CODEC);

    public NormalNoise$NoiseParameters(int firstOctave, List<Double> amplitudes) {
        this.firstOctave = firstOctave;
        this.amplitudes = new DoubleArrayList(amplitudes);
    }

    public NormalNoise$NoiseParameters(int firstOctave, double firstAmplitude, double... amplitudes) {
        this.firstOctave = firstOctave;
        this.amplitudes = new DoubleArrayList(amplitudes);
        this.amplitudes.add(0, firstAmplitude);
    }

    public int firstOctave() {
        return this.firstOctave;
    }

    public DoubleList amplitudes() {
        return this.amplitudes;
    }
}
