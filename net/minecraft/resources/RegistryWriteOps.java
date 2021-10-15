package net.minecraft.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.IRegistryWritable;

public class RegistryWriteOps<T> extends DynamicOpsWrapper<T> {
    private final IRegistryCustom registryAccess;

    public static <T> RegistryWriteOps<T> create(DynamicOps<T> delegate, IRegistryCustom tracker) {
        return new RegistryWriteOps<>(delegate, tracker);
    }

    private RegistryWriteOps(DynamicOps<T> delegate, IRegistryCustom tracker) {
        super(delegate);
        this.registryAccess = tracker;
    }

    protected <E> DataResult<T> encode(E input, T prefix, ResourceKey<? extends IRegistry<E>> registryReference, Codec<E> codec) {
        Optional<IRegistryWritable<E>> optional = this.registryAccess.ownedRegistry(registryReference);
        if (optional.isPresent()) {
            IRegistryWritable<E> writableRegistry = optional.get();
            Optional<ResourceKey<E>> optional2 = writableRegistry.getResourceKey(input);
            if (optional2.isPresent()) {
                ResourceKey<E> resourceKey = optional2.get();
                return MinecraftKey.CODEC.encode(resourceKey.location(), this.delegate, prefix);
            }
        }

        return codec.encode(input, this, prefix);
    }
}
