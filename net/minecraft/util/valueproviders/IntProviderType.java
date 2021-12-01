package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public interface IntProviderType<P extends IntProvider> {
    IntProviderType<IntProviderConstant> CONSTANT = register("constant", IntProviderConstant.CODEC);
    IntProviderType<IntProviderUniform> UNIFORM = register("uniform", IntProviderUniform.CODEC);
    IntProviderType<IntProviderBiasedToBottom> BIASED_TO_BOTTOM = register("biased_to_bottom", IntProviderBiasedToBottom.CODEC);
    IntProviderType<IntProviderClamped> CLAMPED = register("clamped", IntProviderClamped.CODEC);
    IntProviderType<WeightedListInt> WEIGHTED_LIST = register("weighted_list", WeightedListInt.CODEC);
    IntProviderType<ClampedNormalInt> CLAMPED_NORMAL = register("clamped_normal", ClampedNormalInt.CODEC);

    Codec<P> codec();

    static <P extends IntProvider> IntProviderType<P> register(String id, Codec<P> codec) {
        return IRegistry.register(IRegistry.INT_PROVIDER_TYPES, id, () -> {
            return codec;
        });
    }
}
