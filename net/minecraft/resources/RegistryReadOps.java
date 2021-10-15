package net.minecraft.resources;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryReadOps<T> extends DynamicOpsWrapper<T> {
    static final Logger LOGGER = LogManager.getLogger();
    private static final String JSON = ".json";
    private final RegistryReadOps.ResourceAccess resources;
    private final IRegistryCustom registryAccess;
    private final Map<ResourceKey<? extends IRegistry<?>>, RegistryReadOps.ReadCache<?>> readCache;
    private final RegistryReadOps<JsonElement> jsonOps;

    public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> dynamicOps, IResourceManager resourceManager, IRegistryCustom registryAccess) {
        return createAndLoad(dynamicOps, RegistryReadOps.ResourceAccess.forResourceManager(resourceManager), registryAccess);
    }

    public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> dynamicOps, RegistryReadOps.ResourceAccess resourceAccess, IRegistryCustom registryAccess) {
        RegistryReadOps<T> registryReadOps = new RegistryReadOps<>(dynamicOps, resourceAccess, registryAccess, Maps.newIdentityHashMap());
        IRegistryCustom.load(registryAccess, registryReadOps);
        return registryReadOps;
    }

    public static <T> RegistryReadOps<T> create(DynamicOps<T> delegate, IResourceManager resourceManager, IRegistryCustom registryAccess) {
        return create(delegate, RegistryReadOps.ResourceAccess.forResourceManager(resourceManager), registryAccess);
    }

    public static <T> RegistryReadOps<T> create(DynamicOps<T> delegate, RegistryReadOps.ResourceAccess entryLoader, IRegistryCustom registryAccess) {
        return new RegistryReadOps<>(delegate, entryLoader, registryAccess, Maps.newIdentityHashMap());
    }

    private RegistryReadOps(DynamicOps<T> delegate, RegistryReadOps.ResourceAccess entryLoader, IRegistryCustom registryManager, IdentityHashMap<ResourceKey<? extends IRegistry<?>>, RegistryReadOps.ReadCache<?>> valueHolders) {
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
                MinecraftKey resourceLocation = pair.getFirst();
                return this.readAndRegisterElement(key, writableRegistry, codec, resourceLocation).map((supplier) -> {
                    return Pair.of(supplier, pair.getSecond());
                });
            }
        }
    }

    public <E> DataResult<RegistryMaterials<E>> decodeElements(RegistryMaterials<E> registry, ResourceKey<? extends IRegistry<E>> key, Codec<E> codec) {
        Collection<MinecraftKey> collection = this.resources.listResources(key);
        DataResult<RegistryMaterials<E>> dataResult = DataResult.success(registry, Lifecycle.stable());
        String string = key.location().getKey() + "/";

        for(MinecraftKey resourceLocation : collection) {
            String string2 = resourceLocation.getKey();
            if (!string2.endsWith(".json")) {
                LOGGER.warn("Skipping resource {} since it is not a json file", (Object)resourceLocation);
            } else if (!string2.startsWith(string)) {
                LOGGER.warn("Skipping resource {} since it does not have a registry name prefix", (Object)resourceLocation);
            } else {
                String string3 = string2.substring(string.length(), string2.length() - ".json".length());
                MinecraftKey resourceLocation2 = new MinecraftKey(resourceLocation.getNamespace(), string3);
                dataResult = dataResult.flatMap((mappedRegistry) -> {
                    return this.readAndRegisterElement(key, mappedRegistry, codec, resourceLocation2).map((supplier) -> {
                        return mappedRegistry;
                    });
                });
            }
        }

        return dataResult.setPartial(registry);
    }

    private <E> DataResult<Supplier<E>> readAndRegisterElement(ResourceKey<? extends IRegistry<E>> key, IRegistryWritable<E> registry, Codec<E> codec, MinecraftKey elementId) {
        final ResourceKey<E> resourceKey = ResourceKey.create(key, elementId);
        RegistryReadOps.ReadCache<E> readCache = this.readCache(key);
        DataResult<Supplier<E>> dataResult = readCache.values.get(resourceKey);
        if (dataResult != null) {
            return dataResult;
        } else {
            Supplier<E> supplier = Suppliers.memoize(() -> {
                E object = registry.get(resourceKey);
                if (object == null) {
                    throw new RuntimeException("Error during recursive registry parsing, element resolved too early: " + resourceKey);
                } else {
                    return object;
                }
            });
            readCache.values.put(resourceKey, DataResult.success(supplier));
            Optional<DataResult<Pair<E, OptionalInt>>> optional = this.resources.parseElement(this.jsonOps, key, resourceKey, codec);
            DataResult<Supplier<E>> dataResult2;
            if (!optional.isPresent()) {
                dataResult2 = DataResult.success(new Supplier<E>() {
                    @Override
                    public E get() {
                        return registry.get(resourceKey);
                    }

                    @Override
                    public String toString() {
                        return resourceKey.toString();
                    }
                }, Lifecycle.stable());
            } else {
                DataResult<Pair<E, OptionalInt>> dataResult3 = optional.get();
                Optional<Pair<E, OptionalInt>> optional2 = dataResult3.result();
                if (optional2.isPresent()) {
                    Pair<E, OptionalInt> pair = optional2.get();
                    registry.registerOrOverride(pair.getSecond(), resourceKey, pair.getFirst(), dataResult3.lifecycle());
                }

                dataResult2 = dataResult3.map((pairx) -> {
                    return () -> {
                        return registry.get(resourceKey);
                    };
                });
            }

            readCache.values.put(resourceKey, dataResult2);
            return dataResult2;
        }
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

    public interface ResourceAccess {
        Collection<MinecraftKey> listResources(ResourceKey<? extends IRegistry<?>> key);

        <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> dynamicOps, ResourceKey<? extends IRegistry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder);

        static RegistryReadOps.ResourceAccess forResourceManager(IResourceManager resourceManager) {
            return new RegistryReadOps.ResourceAccess() {
                @Override
                public Collection<MinecraftKey> listResources(ResourceKey<? extends IRegistry<?>> key) {
                    return resourceManager.listResources(key.location().getKey(), (name) -> {
                        return name.endsWith(".json");
                    });
                }

                @Override
                public <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> dynamicOps, ResourceKey<? extends IRegistry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder) {
                    MinecraftKey resourceLocation = entryId.location();
                    MinecraftKey resourceLocation2 = new MinecraftKey(resourceLocation.getNamespace(), registryId.location().getKey() + "/" + resourceLocation.getKey() + ".json");
                    if (!resourceManager.hasResource(resourceLocation2)) {
                        return Optional.empty();
                    } else {
                        try {
                            IResource resource = resourceManager.getResource(resourceLocation2);

                            Optional var11;
                            try {
                                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);

                                try {
                                    JsonParser jsonParser = new JsonParser();
                                    JsonElement jsonElement = jsonParser.parse(reader);
                                    var11 = Optional.of(decoder.parse(dynamicOps, jsonElement).map((object) -> {
                                        return Pair.of(object, OptionalInt.empty());
                                    }));
                                } catch (Throwable var14) {
                                    try {
                                        reader.close();
                                    } catch (Throwable var13) {
                                        var14.addSuppressed(var13);
                                    }

                                    throw var14;
                                }

                                reader.close();
                            } catch (Throwable var15) {
                                if (resource != null) {
                                    try {
                                        resource.close();
                                    } catch (Throwable var12) {
                                        var15.addSuppressed(var12);
                                    }
                                }

                                throw var15;
                            }

                            if (resource != null) {
                                resource.close();
                            }

                            return var11;
                        } catch (JsonIOException | JsonSyntaxException | IOException var16) {
                            return Optional.of(DataResult.error("Failed to parse " + resourceLocation2 + " file: " + var16.getMessage()));
                        }
                    }
                }

                @Override
                public String toString() {
                    return "ResourceAccess[" + resourceManager + "]";
                }
            };
        }

        public static final class MemoryMap implements RegistryReadOps.ResourceAccess {
            private final Map<ResourceKey<?>, JsonElement> data = Maps.newIdentityHashMap();
            private final Object2IntMap<ResourceKey<?>> ids = new Object2IntOpenCustomHashMap<>(SystemUtils.identityStrategy());
            private final Map<ResourceKey<?>, Lifecycle> lifecycles = Maps.newIdentityHashMap();

            public <E> void add(IRegistryCustom.Dimension registryManager, ResourceKey<E> key, Encoder<E> encoder, int rawId, E entry, Lifecycle lifecycle) {
                DataResult<JsonElement> dataResult = encoder.encodeStart(RegistryWriteOps.create(JsonOps.INSTANCE, registryManager), entry);
                Optional<PartialResult<JsonElement>> optional = dataResult.error();
                if (optional.isPresent()) {
                    RegistryReadOps.LOGGER.error("Error adding element: {}", (Object)optional.get().message());
                } else {
                    this.data.put(key, dataResult.result().get());
                    this.ids.put(key, rawId);
                    this.lifecycles.put(key, lifecycle);
                }
            }

            @Override
            public Collection<MinecraftKey> listResources(ResourceKey<? extends IRegistry<?>> key) {
                return this.data.keySet().stream().filter((resourceKey2) -> {
                    return resourceKey2.isFor(key);
                }).map((resourceKey2) -> {
                    return new MinecraftKey(resourceKey2.location().getNamespace(), key.location().getKey() + "/" + resourceKey2.location().getKey() + ".json");
                }).collect(Collectors.toList());
            }

            @Override
            public <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> dynamicOps, ResourceKey<? extends IRegistry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder) {
                JsonElement jsonElement = this.data.get(entryId);
                return jsonElement == null ? Optional.of(DataResult.error("Unknown element: " + entryId)) : Optional.of(decoder.parse(dynamicOps, jsonElement).setLifecycle(this.lifecycles.get(entryId)).map((object) -> {
                    return Pair.of(object, OptionalInt.of(this.ids.getInt(entryId)));
                }));
            }
        }
    }
}
