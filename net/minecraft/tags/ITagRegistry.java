package net.minecraft.tags;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ITagRegistry {
    static final Logger LOGGER = LogManager.getLogger();
    public static final ITagRegistry EMPTY = new ITagRegistry(ImmutableMap.of());
    private final Map<ResourceKey<? extends IRegistry<?>>, Tags<?>> collections;

    ITagRegistry(Map<ResourceKey<? extends IRegistry<?>>, Tags<?>> tagGroups) {
        this.collections = tagGroups;
    }

    @Nullable
    private <T> Tags<T> get(ResourceKey<? extends IRegistry<T>> registryKey) {
        return this.collections.get(registryKey);
    }

    public <T> Tags<T> getOrEmpty(ResourceKey<? extends IRegistry<T>> registryKey) {
        return this.collections.getOrDefault(registryKey, Tags.empty());
    }

    public <T, E extends Exception> Tag<T> getTagOrThrow(ResourceKey<? extends IRegistry<T>> registryKey, MinecraftKey id, Function<MinecraftKey, E> exceptionFactory) throws E {
        Tags<T> tagCollection = this.get(registryKey);
        if (tagCollection == null) {
            throw exceptionFactory.apply(id);
        } else {
            Tag<T> tag = tagCollection.getTag(id);
            if (tag == null) {
                throw exceptionFactory.apply(id);
            } else {
                return tag;
            }
        }
    }

    public <T, E extends Exception> MinecraftKey getIdOrThrow(ResourceKey<? extends IRegistry<T>> registryKey, Tag<T> tag, Supplier<E> exceptionSupplier) throws E {
        Tags<T> tagCollection = this.get(registryKey);
        if (tagCollection == null) {
            throw exceptionSupplier.get();
        } else {
            MinecraftKey resourceLocation = tagCollection.getId(tag);
            if (resourceLocation == null) {
                throw exceptionSupplier.get();
            } else {
                return resourceLocation;
            }
        }
    }

    public void getAll(ITagRegistry.CollectionConsumer visitor) {
        this.collections.forEach((type, group) -> {
            acceptCap(visitor, type, group);
        });
    }

    private static <T> void acceptCap(ITagRegistry.CollectionConsumer visitor, ResourceKey<? extends IRegistry<?>> type, Tags<?> group) {
        visitor.accept(type, group);
    }

    public void bind() {
        TagStatic.resetAll(this);
        Blocks.rebuildCache();
    }

    public Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> serializeToNetwork(IRegistryCustom registryManager) {
        final Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> map = Maps.newHashMap();
        this.getAll(new ITagRegistry.CollectionConsumer() {
            @Override
            public <T> void accept(ResourceKey<? extends IRegistry<T>> type, Tags<T> group) {
                Optional<? extends IRegistry<T>> optional = registryManager.registry(type);
                if (optional.isPresent()) {
                    map.put(type, group.serializeToNetwork(optional.get()));
                } else {
                    ITagRegistry.LOGGER.error("Unknown registry {}", (Object)type);
                }

            }
        });
        return map;
    }

    public static ITagRegistry deserializeFromNetwork(IRegistryCustom registryManager, Map<ResourceKey<? extends IRegistry<?>>, Tags.NetworkPayload> groups) {
        ITagRegistry.Builder builder = new ITagRegistry.Builder();
        groups.forEach((type, group) -> {
            addTagsFromPayload(registryManager, builder, type, group);
        });
        return builder.build();
    }

    private static <T> void addTagsFromPayload(IRegistryCustom registryManager, ITagRegistry.Builder builder, ResourceKey<? extends IRegistry<? extends T>> type, Tags.NetworkPayload group) {
        Optional<? extends IRegistry<? extends T>> optional = registryManager.registry(type);
        if (optional.isPresent()) {
            builder.add(type, Tags.createFromNetwork(group, optional.get()));
        } else {
            LOGGER.error("Unknown registry {}", (Object)type);
        }

    }

    public static class Builder {
        private final ImmutableMap.Builder<ResourceKey<? extends IRegistry<?>>, Tags<?>> result = ImmutableMap.builder();

        public <T> ITagRegistry.Builder add(ResourceKey<? extends IRegistry<? extends T>> type, Tags<T> tagGroup) {
            this.result.put(type, tagGroup);
            return this;
        }

        public ITagRegistry build() {
            return new ITagRegistry(this.result.build());
        }
    }

    @FunctionalInterface
    interface CollectionConsumer {
        <T> void accept(ResourceKey<? extends IRegistry<T>> type, Tags<T> group);
    }
}
