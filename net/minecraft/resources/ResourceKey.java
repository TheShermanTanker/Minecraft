package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.IRegistry;

public class ResourceKey<T> {
    private static final Map<String, ResourceKey<?>> VALUES = Collections.synchronizedMap(Maps.newIdentityHashMap());
    private final MinecraftKey registryName;
    private final MinecraftKey location;

    public static <T> Codec<ResourceKey<T>> codec(ResourceKey<? extends IRegistry<T>> registry) {
        return MinecraftKey.CODEC.xmap((id) -> {
            return create(registry, id);
        }, ResourceKey::location);
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends IRegistry<T>> registry, MinecraftKey value) {
        return create(registry.location, value);
    }

    public static <T> ResourceKey<IRegistry<T>> createRegistryKey(MinecraftKey registry) {
        return create(IRegistry.ROOT_REGISTRY_NAME, registry);
    }

    private static <T> ResourceKey<T> create(MinecraftKey registry, MinecraftKey value) {
        String string = (registry + ":" + value).intern();
        return VALUES.computeIfAbsent(string, (stringx) -> {
            return new ResourceKey(registry, value);
        });
    }

    private ResourceKey(MinecraftKey registry, MinecraftKey value) {
        this.registryName = registry;
        this.location = value;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + this.registryName + " / " + this.location + "]";
    }

    public boolean isFor(ResourceKey<? extends IRegistry<?>> registry) {
        return this.registryName.equals(registry.location());
    }

    public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends IRegistry<E>> resourceKey) {
        return this.isFor(resourceKey) ? Optional.of(this) : Optional.empty();
    }

    public MinecraftKey location() {
        return this.location;
    }

    public static <T> Function<MinecraftKey, ResourceKey<T>> elementKey(ResourceKey<? extends IRegistry<T>> registry) {
        return (id) -> {
            return create(registry, id);
        };
    }
}
