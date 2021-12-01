package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;

public class StructureSettingsFeature {
    public static final Codec<StructureSettingsFeature> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(0, 4096).fieldOf("spacing").forGetter((config) -> {
            return config.spacing;
        }), Codec.intRange(0, 4096).fieldOf("separation").forGetter((config) -> {
            return config.separation;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter((config) -> {
            return config.salt;
        })).apply(instance, StructureSettingsFeature::new);
    }).comapFlatMap((config) -> {
        return config.spacing <= config.separation ? DataResult.error("Spacing has to be larger than separation") : DataResult.success(config);
    }, Function.identity());
    private final int spacing;
    private final int separation;
    private final int salt;

    public StructureSettingsFeature(int spacing, int separation, int salt) {
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
    }

    public int spacing() {
        return this.spacing;
    }

    public int separation() {
        return this.separation;
    }

    public int salt() {
        return this.salt;
    }
}
