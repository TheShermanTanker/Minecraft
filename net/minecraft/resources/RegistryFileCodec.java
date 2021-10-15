package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;

public final class RegistryFileCodec<E> implements Codec<Supplier<E>> {
    private final ResourceKey<? extends IRegistry<E>> registryKey;
    private final Codec<E> elementCodec;
    private final boolean allowInline;

    public static <E> RegistryFileCodec<E> create(ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> elementCodec) {
        return create(registryRef, elementCodec, true);
    }

    public static <E> Codec<List<Supplier<E>>> homogeneousList(ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> elementCodec) {
        return Codec.either(create(registryRef, elementCodec, false).listOf(), elementCodec.xmap((object) -> {
            return () -> {
                return object;
            };
        }, Supplier::get).listOf()).xmap((either) -> {
            return either.map((list) -> {
                return list;
            }, (list) -> {
                return list;
            });
        }, Either::left);
    }

    private static <E> RegistryFileCodec<E> create(ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> elementCodec, boolean allowInlineDefinitions) {
        return new RegistryFileCodec<>(registryRef, elementCodec, allowInlineDefinitions);
    }

    private RegistryFileCodec(ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> elementCodec, boolean allowInlineDefinitions) {
        this.registryKey = registryRef;
        this.elementCodec = elementCodec;
        this.allowInline = allowInlineDefinitions;
    }

    @Override
    public <T> DataResult<T> encode(Supplier<E> supplier, DynamicOps<T> dynamicOps, T object) {
        return dynamicOps instanceof RegistryWriteOps ? ((RegistryWriteOps)dynamicOps).encode(supplier.get(), object, this.registryKey, this.elementCodec) : this.elementCodec.encode(supplier.get(), dynamicOps, object);
    }

    @Override
    public <T> DataResult<Pair<Supplier<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        return dynamicOps instanceof RegistryReadOps ? ((RegistryReadOps)dynamicOps).decodeElement(object, this.registryKey, this.elementCodec, this.allowInline) : this.elementCodec.decode(dynamicOps, object).map((pair) -> {
            return pair.mapFirst((object) -> {
                return () -> {
                    return object;
                };
            });
        });
    }

    @Override
    public String toString() {
        return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
    }
}
