package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public interface FloatProviderType<P extends FloatProvider> {
    FloatProviderType<FloatProviderConstant> CONSTANT = register("constant", FloatProviderConstant.CODEC);
    FloatProviderType<FloatProviderUniform> UNIFORM = register("uniform", FloatProviderUniform.CODEC);
    FloatProviderType<FloatProviderClampedNormal> CLAMPED_NORMAL = register("clamped_normal", FloatProviderClampedNormal.CODEC);
    FloatProviderType<FloatProviderTrapezoid> TRAPEZOID = register("trapezoid", FloatProviderTrapezoid.CODEC);

    Codec<P> codec();

    static <P extends FloatProvider> FloatProviderType<P> register(String id, Codec<P> codec) {
        return IRegistry.register(IRegistry.FLOAT_PROVIDER_TYPES, id, () -> {
            return codec;
        });
    }
}
