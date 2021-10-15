package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;

public abstract class IRegistryWritable<T> extends IRegistry<T> {
    public IRegistryWritable(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle) {
        super(key, lifecycle);
    }

    public abstract <V extends T> V registerMapping(int rawId, ResourceKey<T> key, V entry, Lifecycle lifecycle);

    public abstract <V extends T> V register(ResourceKey<T> key, V entry, Lifecycle lifecycle);

    public abstract <V extends T> V registerOrOverride(OptionalInt rawId, ResourceKey<T> key, V newEntry, Lifecycle lifecycle);

    public abstract boolean isEmpty();
}
