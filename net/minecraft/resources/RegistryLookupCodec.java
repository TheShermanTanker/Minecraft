package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import net.minecraft.core.IRegistry;

public final class RegistryLookupCodec<E> extends MapCodec<IRegistry<E>> {
    private final ResourceKey<? extends IRegistry<E>> registryKey;

    public static <E> RegistryLookupCodec<E> create(ResourceKey<? extends IRegistry<E>> registryKey) {
        return new RegistryLookupCodec<>(registryKey);
    }

    private RegistryLookupCodec(ResourceKey<? extends IRegistry<E>> registryKey) {
        this.registryKey = registryKey;
    }

    public <T> RecordBuilder<T> encode(IRegistry<E> registry, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
        return recordBuilder;
    }

    public <T> DataResult<IRegistry<E>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
        return dynamicOps instanceof RegistryReadOps ? ((RegistryReadOps)dynamicOps).registry(this.registryKey) : DataResult.error("Not a registry ops");
    }

    public String toString() {
        return "RegistryLookupCodec[" + this.registryKey + "]";
    }

    public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
        return Stream.empty();
    }
}
