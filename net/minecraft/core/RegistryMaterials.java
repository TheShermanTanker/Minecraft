package net.minecraft.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryDataPackCodec;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryMaterials<T> extends IRegistryWritable<T> {
    protected static final Logger LOGGER = LogManager.getLogger();
    private final ObjectList<T> byId = new ObjectArrayList<>(256);
    private final Object2IntMap<T> toId = new Object2IntOpenCustomHashMap<>(SystemUtils.identityStrategy());
    private final BiMap<MinecraftKey, T> storage;
    private final BiMap<ResourceKey<T>, T> keyStorage;
    private final Map<T, Lifecycle> lifecycles;
    private Lifecycle elementsLifecycle;
    protected Object[] randomCache;
    private int nextId;

    public RegistryMaterials(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle) {
        super(key, lifecycle);
        this.toId.defaultReturnValue(-1);
        this.storage = HashBiMap.create();
        this.keyStorage = HashBiMap.create();
        this.lifecycles = Maps.newIdentityHashMap();
        this.elementsLifecycle = lifecycle;
    }

    public static <T> MapCodec<RegistryMaterials.RegistryEntry<T>> withNameAndId(ResourceKey<? extends IRegistry<T>> key, MapCodec<T> entryCodec) {
        return RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(MinecraftKey.CODEC.xmap(ResourceKey.elementKey(key), ResourceKey::location).fieldOf("name").forGetter((registryEntry) -> {
                return registryEntry.key;
            }), Codec.INT.fieldOf("id").forGetter((registryEntry) -> {
                return registryEntry.id;
            }), entryCodec.forGetter((registryEntry) -> {
                return registryEntry.value;
            })).apply(instance, RegistryMaterials.RegistryEntry::new);
        });
    }

    @Override
    public <V extends T> V registerMapping(int rawId, ResourceKey<T> key, V entry, Lifecycle lifecycle) {
        return this.registerMapping(rawId, key, entry, lifecycle, true);
    }

    private <V extends T> V registerMapping(int rawId, ResourceKey<T> key, V entry, Lifecycle lifecycle, boolean checkDuplicateKeys) {
        Validate.notNull(key);
        Validate.notNull((T)entry);
        this.byId.size(Math.max(this.byId.size(), rawId + 1));
        this.byId.set(rawId, entry);
        this.toId.put((T)entry, rawId);
        this.randomCache = null;
        if (checkDuplicateKeys && this.keyStorage.containsKey(key)) {
            LOGGER.debug("Adding duplicate key '{}' to registry", (Object)key);
        }

        if (this.storage.containsValue(entry)) {
            LOGGER.error("Adding duplicate value '{}' to registry", entry);
        }

        this.storage.put(key.location(), (T)entry);
        this.keyStorage.put(key, (T)entry);
        this.lifecycles.put((T)entry, lifecycle);
        this.elementsLifecycle = this.elementsLifecycle.add(lifecycle);
        if (this.nextId <= rawId) {
            this.nextId = rawId + 1;
        }

        return entry;
    }

    @Override
    public <V extends T> V register(ResourceKey<T> key, V entry, Lifecycle lifecycle) {
        return this.registerMapping(this.nextId, key, entry, lifecycle);
    }

    @Override
    public <V extends T> V registerOrOverride(OptionalInt rawId, ResourceKey<T> key, V newEntry, Lifecycle lifecycle) {
        Validate.notNull(key);
        Validate.notNull((T)newEntry);
        T object = this.keyStorage.get(key);
        int i;
        if (object == null) {
            i = rawId.isPresent() ? rawId.getAsInt() : this.nextId;
        } else {
            i = this.toId.getInt(object);
            if (rawId.isPresent() && rawId.getAsInt() != i) {
                throw new IllegalStateException("ID mismatch");
            }

            this.toId.removeInt(object);
            this.lifecycles.remove(object);
        }

        return this.registerMapping(i, key, newEntry, lifecycle, false);
    }

    @Nullable
    @Override
    public MinecraftKey getKey(T entry) {
        return this.storage.inverse().get(entry);
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T entry) {
        return Optional.ofNullable(this.keyStorage.inverse().get(entry));
    }

    @Override
    public int getId(@Nullable T entry) {
        return this.toId.getInt(entry);
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceKey<T> key) {
        return this.keyStorage.get(key);
    }

    @Nullable
    @Override
    public T fromId(int index) {
        return (T)(index >= 0 && index < this.byId.size() ? this.byId.get(index) : null);
    }

    @Override
    public Lifecycle lifecycle(T entry) {
        return this.lifecycles.get(entry);
    }

    @Override
    public Lifecycle elementsLifecycle() {
        return this.elementsLifecycle;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.byId.iterator(), Objects::nonNull);
    }

    @Nullable
    @Override
    public T get(@Nullable MinecraftKey id) {
        return this.storage.get(id);
    }

    @Override
    public Set<MinecraftKey> keySet() {
        return Collections.unmodifiableSet(this.storage.keySet());
    }

    @Override
    public Set<Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.unmodifiableMap(this.keyStorage).entrySet();
    }

    @Override
    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    @Nullable
    @Override
    public T getRandom(Random random) {
        if (this.randomCache == null) {
            Collection<?> collection = this.storage.values();
            if (collection.isEmpty()) {
                return (T)null;
            }

            this.randomCache = collection.toArray(new Object[collection.size()]);
        }

        return SystemUtils.getRandom((T[])this.randomCache, random);
    }

    @Override
    public boolean containsKey(MinecraftKey id) {
        return this.storage.containsKey(id);
    }

    @Override
    public boolean containsKey(ResourceKey<T> key) {
        return this.keyStorage.containsKey(key);
    }

    public static <T> Codec<RegistryMaterials<T>> networkCodec(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle, Codec<T> entryCodec) {
        return withNameAndId(key, entryCodec.fieldOf("element")).codec().listOf().xmap((list) -> {
            RegistryMaterials<T> mappedRegistry = new RegistryMaterials<>(key, lifecycle);

            for(RegistryMaterials.RegistryEntry<T> registryEntry : list) {
                mappedRegistry.registerMapping(registryEntry.id, registryEntry.key, registryEntry.value, lifecycle);
            }

            return mappedRegistry;
        }, (mappedRegistry) -> {
            Builder<RegistryMaterials.RegistryEntry<T>> builder = ImmutableList.builder();

            for(T object : mappedRegistry) {
                builder.add(new RegistryMaterials.RegistryEntry<>(mappedRegistry.getResourceKey(object).get(), mappedRegistry.getId(object), object));
            }

            return builder.build();
        });
    }

    public static <T> Codec<RegistryMaterials<T>> dataPackCodec(ResourceKey<? extends IRegistry<T>> registryRef, Lifecycle lifecycle, Codec<T> entryCodec) {
        return RegistryDataPackCodec.create(registryRef, lifecycle, entryCodec);
    }

    public static <T> Codec<RegistryMaterials<T>> directCodec(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle, Codec<T> entryCodec) {
        return Codec.unboundedMap(MinecraftKey.CODEC.xmap(ResourceKey.elementKey(key), ResourceKey::location), entryCodec).xmap((map) -> {
            RegistryMaterials<T> mappedRegistry = new RegistryMaterials<>(key, lifecycle);
            map.forEach((resourceKey, object) -> {
                mappedRegistry.register(resourceKey, object, lifecycle);
            });
            return mappedRegistry;
        }, (mappedRegistry) -> {
            return ImmutableMap.copyOf(mappedRegistry.keyStorage);
        });
    }

    public static class RegistryEntry<T> {
        public final ResourceKey<T> key;
        public final int id;
        public final T value;

        public RegistryEntry(ResourceKey<T> key, int rawId, T entry) {
            this.key = key;
            this.id = rawId;
            this.value = entry;
        }
    }
}
