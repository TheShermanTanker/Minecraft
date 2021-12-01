package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.IRegistry;
import net.minecraft.core.RegistryMaterials;

public final class RegistryDataPackCodec<E> implements Codec<RegistryMaterials<E>> {
    private final Codec<RegistryMaterials<E>> directCodec;
    private final ResourceKey<? extends IRegistry<E>> registryKey;
    private final Codec<E> elementCodec;

    public static <E> RegistryDataPackCodec<E> create(ResourceKey<? extends IRegistry<E>> registryRef, Lifecycle lifecycle, Codec<E> codec) {
        return new RegistryDataPackCodec<>(registryRef, lifecycle, codec);
    }

    private RegistryDataPackCodec(ResourceKey<? extends IRegistry<E>> registryRef, Lifecycle lifecycle, Codec<E> codec) {
        this.directCodec = RegistryMaterials.directCodec(registryRef, lifecycle, codec);
        this.registryKey = registryRef;
        this.elementCodec = codec;
    }

    public <T> DataResult<T> encode(RegistryMaterials<E> mappedRegistry, DynamicOps<T> dynamicOps, T object) {
        return this.directCodec.encode(mappedRegistry, dynamicOps, object);
    }

    public <T> DataResult<Pair<RegistryMaterials<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        DataResult<Pair<RegistryMaterials<E>, T>> dataResult = this.directCodec.decode(dynamicOps, object);
        return dynamicOps instanceof RegistryReadOps ? dataResult.flatMap((pair) -> {
            return ((RegistryReadOps)dynamicOps).decodeElements(pair.getFirst(), this.registryKey, this.elementCodec).map((mappedRegistry) -> {
                return Pair.of(mappedRegistry, (T)pair.getSecond());
            });
        }) : dataResult;
    }

    @Override
    public String toString() {
        return "RegistryDataPackCodec[" + this.directCodec + " " + this.registryKey + " " + this.elementCodec + "]";
    }
}
