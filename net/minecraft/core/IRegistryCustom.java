package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureStructureProcessorType;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IRegistryCustom {
    private static final Logger LOGGER = LogManager.getLogger();
    static final Map<ResourceKey<? extends IRegistry<?>>, IRegistryCustom.RegistryData<?>> REGISTRIES = SystemUtils.make(() -> {
        Builder<ResourceKey<? extends IRegistry<?>>, IRegistryCustom.RegistryData<?>> builder = ImmutableMap.builder();
        put(builder, IRegistry.DIMENSION_TYPE_REGISTRY, DimensionManager.DIRECT_CODEC, DimensionManager.DIRECT_CODEC);
        put(builder, IRegistry.BIOME_REGISTRY, BiomeBase.DIRECT_CODEC, BiomeBase.NETWORK_CODEC);
        put(builder, IRegistry.CONFIGURED_SURFACE_BUILDER_REGISTRY, WorldGenSurfaceComposite.DIRECT_CODEC);
        put(builder, IRegistry.CONFIGURED_CARVER_REGISTRY, WorldGenCarverWrapper.DIRECT_CODEC);
        put(builder, IRegistry.CONFIGURED_FEATURE_REGISTRY, WorldGenFeatureConfigured.DIRECT_CODEC);
        put(builder, IRegistry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, StructureFeature.DIRECT_CODEC);
        put(builder, IRegistry.PROCESSOR_LIST_REGISTRY, DefinedStructureStructureProcessorType.DIRECT_CODEC);
        put(builder, IRegistry.TEMPLATE_POOL_REGISTRY, WorldGenFeatureDefinedStructurePoolTemplate.DIRECT_CODEC);
        put(builder, IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, GeneratorSettingBase.DIRECT_CODEC);
        return builder.build();
    });
    private static final IRegistryCustom.Dimension BUILTIN = SystemUtils.make(() -> {
        IRegistryCustom.Dimension registryHolder = new IRegistryCustom.Dimension();
        DimensionManager.registerBuiltin(registryHolder);
        REGISTRIES.keySet().stream().filter((resourceKey) -> {
            return !resourceKey.equals(IRegistry.DIMENSION_TYPE_REGISTRY);
        }).forEach((resourceKey) -> {
            copyBuiltin(registryHolder, resourceKey);
        });
        return registryHolder;
    });

    public abstract <E> Optional<IRegistryWritable<E>> ownedRegistry(ResourceKey<? extends IRegistry<? extends E>> key);

    public <E> IRegistryWritable<E> ownedRegistryOrThrow(ResourceKey<? extends IRegistry<? extends E>> key) {
        return this.ownedRegistry(key).orElseThrow(() -> {
            return new IllegalStateException("Missing registry: " + key);
        });
    }

    public <E> Optional<? extends IRegistry<E>> registry(ResourceKey<? extends IRegistry<? extends E>> key) {
        Optional<? extends IRegistry<E>> optional = this.ownedRegistry(key);
        return optional.isPresent() ? optional : IRegistry.REGISTRY.getOptional(key.location());
    }

    public <E> IRegistry<E> registryOrThrow(ResourceKey<? extends IRegistry<? extends E>> key) {
        return this.registry(key).orElseThrow(() -> {
            return new IllegalStateException("Missing registry: " + key);
        });
    }

    private static <E> void put(Builder<ResourceKey<? extends IRegistry<?>>, IRegistryCustom.RegistryData<?>> infosBuilder, ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> entryCodec) {
        infosBuilder.put(registryRef, new IRegistryCustom.RegistryData<>(registryRef, entryCodec, (Codec<E>)null));
    }

    private static <E> void put(Builder<ResourceKey<? extends IRegistry<?>>, IRegistryCustom.RegistryData<?>> infosBuilder, ResourceKey<? extends IRegistry<E>> registryRef, Codec<E> entryCodec, Codec<E> networkEntryCodec) {
        infosBuilder.put(registryRef, new IRegistryCustom.RegistryData<>(registryRef, entryCodec, networkEntryCodec));
    }

    public static IRegistryCustom.Dimension builtin() {
        IRegistryCustom.Dimension registryHolder = new IRegistryCustom.Dimension();
        RegistryReadOps.ResourceAccess.MemoryMap memoryMap = new RegistryReadOps.ResourceAccess.MemoryMap();

        for(IRegistryCustom.RegistryData<?> registryData : REGISTRIES.values()) {
            addBuiltinElements(registryHolder, memoryMap, registryData);
        }

        RegistryReadOps.createAndLoad(JsonOps.INSTANCE, memoryMap, registryHolder);
        return registryHolder;
    }

    private static <E> void addBuiltinElements(IRegistryCustom.Dimension registryManager, RegistryReadOps.ResourceAccess.MemoryMap entryLoader, IRegistryCustom.RegistryData<E> info) {
        ResourceKey<? extends IRegistry<E>> resourceKey = info.key();
        boolean bl = !resourceKey.equals(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY) && !resourceKey.equals(IRegistry.DIMENSION_TYPE_REGISTRY);
        IRegistry<E> registry = BUILTIN.registryOrThrow(resourceKey);
        IRegistryWritable<E> writableRegistry = registryManager.ownedRegistryOrThrow(resourceKey);

        for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
            ResourceKey<E> resourceKey2 = entry.getKey();
            E object = entry.getValue();
            if (bl) {
                entryLoader.add(BUILTIN, resourceKey2, info.codec(), registry.getId(object), object, registry.lifecycle(object));
            } else {
                writableRegistry.registerMapping(registry.getId(object), resourceKey2, object, registry.lifecycle(object));
            }
        }

    }

    private static <R extends IRegistry<?>> void copyBuiltin(IRegistryCustom.Dimension manager, ResourceKey<R> registryRef) {
        IRegistry<R> registry = RegistryGeneration.REGISTRY;
        IRegistry<?> registry2 = registry.getOrThrow(registryRef);
        copy(manager, registry2);
    }

    private static <E> void copy(IRegistryCustom.Dimension manager, IRegistry<E> registry) {
        IRegistryWritable<E> writableRegistry = manager.ownedRegistryOrThrow(registry.key());

        for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
            E object = entry.getValue();
            writableRegistry.registerMapping(registry.getId(object), entry.getKey(), object, registry.lifecycle(object));
        }

    }

    public static void load(IRegistryCustom registryAccess, RegistryReadOps<?> registryReadOps) {
        for(IRegistryCustom.RegistryData<?> registryData : REGISTRIES.values()) {
            readRegistry(registryReadOps, registryAccess, registryData);
        }

    }

    private static <E> void readRegistry(RegistryReadOps<?> ops, IRegistryCustom registryAccess, IRegistryCustom.RegistryData<E> info) {
        ResourceKey<? extends IRegistry<E>> resourceKey = info.key();
        RegistryMaterials<E> mappedRegistry = (RegistryMaterials)registryAccess.<E>ownedRegistryOrThrow(resourceKey);
        DataResult<RegistryMaterials<E>> dataResult = ops.decodeElements(mappedRegistry, info.key(), info.codec());
        dataResult.error().ifPresent((partialResult) -> {
            throw new JsonParseException("Error loading registry data: " + partialResult.message());
        });
    }

    public static final class Dimension extends IRegistryCustom {
        public static final Codec<IRegistryCustom.Dimension> NETWORK_CODEC = makeNetworkCodec();
        private final Map<? extends ResourceKey<? extends IRegistry<?>>, ? extends RegistryMaterials<?>> registries;

        private static <E> Codec<IRegistryCustom.Dimension> makeNetworkCodec() {
            Codec<ResourceKey<? extends IRegistry<E>>> codec = MinecraftKey.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);
            Codec<RegistryMaterials<E>> codec2 = codec.partialDispatch("type", (mappedRegistry) -> {
                return DataResult.success(mappedRegistry.key());
            }, (resourceKey) -> {
                return getNetworkCodec(resourceKey).map((codec) -> {
                    return RegistryMaterials.networkCodec(resourceKey, Lifecycle.experimental(), codec);
                });
            });
            UnboundedMapCodec<? extends ResourceKey<? extends IRegistry<?>>, ? extends RegistryMaterials<?>> unboundedMapCodec = Codec.unboundedMap(codec, codec2);
            return captureMap(unboundedMapCodec);
        }

        private static <K extends ResourceKey<? extends IRegistry<?>>, V extends RegistryMaterials<?>> Codec<IRegistryCustom.Dimension> captureMap(UnboundedMapCodec<K, V> unboundedMapCodec) {
            return unboundedMapCodec.xmap(IRegistryCustom.Dimension::new, (registryHolder) -> {
                return registryHolder.registries.entrySet().stream().filter((entry) -> {
                    return IRegistryCustom.REGISTRIES.get(entry.getKey()).sendToClient();
                }).collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
            });
        }

        private static <E> DataResult<? extends Codec<E>> getNetworkCodec(ResourceKey<? extends IRegistry<E>> registryRef) {
            return Optional.ofNullable(IRegistryCustom.REGISTRIES.get(registryRef)).map((registryData) -> {
                return registryData.networkCodec();
            }).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown or not serializable registry: " + registryRef);
            });
        }

        public Dimension() {
            this(IRegistryCustom.REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), IRegistryCustom.Dimension::createRegistry)));
        }

        private Dimension(Map<? extends ResourceKey<? extends IRegistry<?>>, ? extends RegistryMaterials<?>> registries) {
            this.registries = registries;
        }

        private static <E> RegistryMaterials<?> createRegistry(ResourceKey<? extends IRegistry<?>> registryRef) {
            return new RegistryMaterials<>(registryRef, Lifecycle.stable());
        }

        @Override
        public <E> Optional<IRegistryWritable<E>> ownedRegistry(ResourceKey<? extends IRegistry<? extends E>> key) {
            return Optional.ofNullable(this.registries.get(key)).map((mappedRegistry) -> {
                return mappedRegistry;
            });
        }
    }

    static final class RegistryData<E> {
        private final ResourceKey<? extends IRegistry<E>> key;
        private final Codec<E> codec;
        @Nullable
        private final Codec<E> networkCodec;

        public RegistryData(ResourceKey<? extends IRegistry<E>> registry, Codec<E> entryCodec, @Nullable Codec<E> networkEntryCodec) {
            this.key = registry;
            this.codec = entryCodec;
            this.networkCodec = networkEntryCodec;
        }

        public ResourceKey<? extends IRegistry<E>> key() {
            return this.key;
        }

        public Codec<E> codec() {
            return this.codec;
        }

        @Nullable
        public Codec<E> networkCodec() {
            return this.networkCodec;
        }

        public boolean sendToClient() {
            return this.networkCodec != null;
        }
    }
}
