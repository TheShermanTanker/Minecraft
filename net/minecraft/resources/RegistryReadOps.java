package net.minecraft.resources;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.server.packs.resources.IResourceManager;

public class RegistryReadOps<T> extends DynamicOpsWrapper<T> {
    private final RegistryResourceAccess resources;
    private final IRegistryCustom registryAccess;
    private final Map<ResourceKey<? extends IRegistry<?>>, RegistryReadOps.ReadCache<?>> readCache;
    private final RegistryReadOps<JsonElement> jsonOps;

    public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> ops, IResourceManager resourceManager, IRegistryCustom registryManager) {
        return createAndLoad(ops, RegistryResourceAccess.forResourceManager(resourceManager), registryManager);
    }

    public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> ops, RegistryResourceAccess entryLoader, IRegistryCustom registryManager) {
        RegistryReadOps<T> registryReadOps = new RegistryReadOps<>(ops, entryLoader, registryManager, Maps.newIdentityHashMap());
        IRegistryCustom.load(registryManager, registryReadOps);
        return registryReadOps;
    }

    public static <T> RegistryReadOps<T> create(DynamicOps<T> delegate, IResourceManager resourceManager, IRegistryCustom registryManager) {
        return create(delegate, RegistryResourceAccess.forResourceManager(resourceManager), registryManager);
    }

    public static <T> RegistryReadOps<T> create(DynamicOps<T> delegate, RegistryResourceAccess entryLoader, IRegistryCustom registryManager) {
        return new RegistryReadOps<>(delegate, entryLoader, registryManager, Maps.newIdentityHashMap());
    }

    private RegistryReadOps(DynamicOps<T> delegate, RegistryResourceAccess entryLoader, IRegistryCustom registryManager, IdentityHashMap<ResourceKey<? extends IRegistry<?>>, RegistryReadOps.ReadCache<?>> valueHolders) {
        super(delegate);
        this.resources = entryLoader;
        this.registryAccess = registryManager;
        this.readCache = valueHolders;
        this.jsonOps = delegate == JsonOps.INSTANCE ? this : new RegistryReadOps<>(JsonOps.INSTANCE, entryLoader, registryManager, valueHolders);
    }

    protected <E> DataResult<Pair<Supplier<E>, T>> decodeElement(T object, ResourceKey<? extends IRegistry<E>> key, Codec<E> codec, boolean allowInlineDefinitions) {
        Optional<IRegistryWritable<E>> optional = this.registryAccess.ownedRegistry(key);
        if (!optional.isPresent()) {
            return DataResult.error("Unknown registry: " + key);
        } else {
            IRegistryWritable<E> writableRegistry = optional.get();
            DataResult<Pair<MinecraftKey, T>> dataResult = MinecraftKey.CODEC.decode(this.delegate, object);
            if (!dataResult.result().isPresent()) {
                return !allowInlineDefinitions ? DataResult.error("Inline definitions not allowed here") : codec.decode(this, object).map((pairx) -> {
                    return pairx.mapFirst((object) -> {
                        return () -> {
                            return object;
                        };
                    });
                });
            } else {
                Pair<MinecraftKey, T> pair = dataResult.result().get();
                ResourceKey<E> resourceKey = ResourceKey.create(key, pair.getFirst());
                return this.readAndRegisterElement(key, writableRegistry, codec, resourceKey).map((supplier) -> {
                    return Pair.of(supplier, pair.getSecond());
                });
            }
        }
    }

    public <E> DataResult<RegistryMaterials<E>> decodeElements(RegistryMaterials<E> registry, ResourceKey<? extends IRegistry<E>> key, Codec<E> codec) {
        Collection<ResourceKey<E>> collection = this.resources.listResources(key);
        DataResult<RegistryMaterials<E>> dataResult = DataResult.success(registry, Lifecycle.stable());

        for(ResourceKey<E> resourceKey : collection) {
            dataResult = dataResult.flatMap((mappedRegistry) -> {
                return this.readAndRegisterElement(key, mappedRegistry, codec, resourceKey).map((supplier) -> {
                    return mappedRegistry;
                });
            });
        }

        return dataResult.setPartial(registry);
    }

    private <E> DataResult<Supplier<E>> readAndRegisterElement(ResourceKey<? extends IRegistry<E>> resourceKey, IRegistryWritable<E> writableRegistry, Codec<E> codec, ResourceKey<E> resourceKey2) {
        RegistryReadOps.ReadCache<E> readCache = this.readCache(resourceKey);
        DataResult<Supplier<E>> dataResult = readCache.values.get(resourceKey2);
        if (dataResult != null) {
            return dataResult;
        } else {
            readCache.values.put(resourceKey2, DataResult.success(createPlaceholderGetter(writableRegistry, resourceKey2)));
            Optional<DataResult<RegistryResourceAccess.ParsedEntry<E>>> optional = this.resources.parseElement(this.jsonOps, resourceKey, resourceKey2, codec);
            DataResult<Supplier<E>> dataResult2;
            if (optional.isEmpty()) {
                if (writableRegistry.containsKey(resourceKey2)) {
                    dataResult2 = DataResult.success(createRegistryGetter(writableRegistry, resourceKey2), Lifecycle.stable());
                } else {
                    dataResult2 = DataResult.error("Missing referenced custom/removed registry entry for registry " + resourceKey + " named " + resourceKey2.location());
                }
            } else {
                DataResult<RegistryResourceAccess.ParsedEntry<E>> dataResult4 = optional.get();
                Optional<RegistryResourceAccess.ParsedEntry<E>> optional2 = dataResult4.result();
                if (optional2.isPresent()) {
                    RegistryResourceAccess.ParsedEntry<E> parsedEntry = optional2.get();
                    writableRegistry.registerOrOverride(parsedEntry.fixedId(), resourceKey2, parsedEntry.value(), dataResult4.lifecycle());
                }

                dataResult2 = dataResult4.map((parsedEntryx) -> {
                    return createRegistryGetter(writableRegistry, resourceKey2);
                });
            }

            readCache.values.put(resourceKey2, dataResult2);
            return dataResult2;
        }
    }

    private static <E> Supplier<E> createPlaceholderGetter(IRegistryWritable<E> registry, ResourceKey<E> key) {
        return Suppliers.memoize(() -> {
            E object = registry.get(key);
            if (object == null) {
                throw new RuntimeException("Error during recursive registry parsing, element resolved too early: " + key);
            } else {
                return object;
            }
        });
    }

    private static <E> Supplier<E> createRegistryGetter(IRegistry<E> registry, ResourceKey<E> key) {
        return new Supplier<E>() {
            @Override
            public E get() {
                return registry.get(key);
            }

            @Override
            public String toString() {
                return key.toString();
            }
        };
    }

    private <E> RegistryReadOps.ReadCache<E> readCache(ResourceKey<? extends IRegistry<E>> registryRef) {
        return this.readCache.computeIfAbsent(registryRef, (resourceKey) -> {
            return new RegistryReadOps.ReadCache();
        });
    }

    protected <E> DataResult<IRegistry<E>> registry(ResourceKey<? extends IRegistry<E>> key) {
        return this.registryAccess.ownedRegistry(key).map((writableRegistry) -> {
            return DataResult.success(writableRegistry, writableRegistry.elementsLifecycle());
        }).orElseGet(() -> {
            return DataResult.error("Unknown registry: " + key);
        });
    }

    static final class ReadCache<E> {
        final Map<ResourceKey<E>, DataResult<Supplier<E>>> values = Maps.newIdentityHashMap();
    }
}
