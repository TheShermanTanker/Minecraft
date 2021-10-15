package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public interface IntProviderType<P extends IntProvider> {
    IntProviderType<ConstantInt> CONSTANT = register("constant", ConstantInt.CODEC);
    IntProviderType<UniformInt> UNIFORM = register("uniform", UniformInt.CODEC);
    IntProviderType<BiasedToBottomInt> BIASED_TO_BOTTOM = register("biased_to_bottom", BiasedToBottomInt.CODEC);
    IntProviderType<ClampedInt> CLAMPED = register("clamped", ClampedInt.CODEC);

    Codec<P> codec();

    static <P extends IntProvider> IntProviderType<P> register(String id, Codec<P> codec) {
        return IRegistry.register(IRegistry.INT_PROVIDER_TYPES, id, () -> {
            return codec;
        });
    }
}
