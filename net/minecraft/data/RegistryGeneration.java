package net.minecraft.data;

import com.google.common.collect.Maps;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.data.worldgen.WorldGenCarvers;
import net.minecraft.data.worldgen.WorldGenFeaturePieces;
import net.minecraft.data.worldgen.WorldGenSurfaceComposites;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryGeneration {
    protected static final Logger LOGGER = LogManager.getLogger();
    private static final Map<MinecraftKey, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
    private static final IRegistryWritable<IRegistryWritable<?>> WRITABLE_REGISTRY = new RegistryMaterials<>(ResourceKey.createRegistryKey(new MinecraftKey("root")), Lifecycle.experimental());
    public static final IRegistry<? extends IRegistry<?>> REGISTRY = WRITABLE_REGISTRY;
    public static final IRegistry<WorldGenSurfaceComposite<?>> CONFIGURED_SURFACE_BUILDER = registerSimple(IRegistry.CONFIGURED_SURFACE_BUILDER_REGISTRY, () -> {
        return WorldGenSurfaceComposites.NOPE;
    });
    public static final IRegistry<WorldGenCarverWrapper<?>> CONFIGURED_CARVER = registerSimple(IRegistry.CONFIGURED_CARVER_REGISTRY, () -> {
        return WorldGenCarvers.CAVE;
    });
    public static final IRegistry<WorldGenFeatureConfigured<?, ?>> CONFIGURED_FEATURE = registerSimple(IRegistry.CONFIGURED_FEATURE_REGISTRY, () -> {
        return BiomeDecoratorGroups.OAK;
    });
    public static final IRegistry<StructureFeature<?, ?>> CONFIGURED_STRUCTURE_FEATURE = registerSimple(IRegistry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, () -> {
        return StructureFeatures.MINESHAFT;
    });
    public static final IRegistry<ProcessorList> PROCESSOR_LIST = registerSimple(IRegistry.PROCESSOR_LIST_REGISTRY, () -> {
        return ProcessorLists.ZOMBIE_PLAINS;
    });
    public static final IRegistry<WorldGenFeatureDefinedStructurePoolTemplate> TEMPLATE_POOL = registerSimple(IRegistry.TEMPLATE_POOL_REGISTRY, WorldGenFeaturePieces::bootstrap);
    public static final IRegistry<BiomeBase> BIOME = registerSimple(IRegistry.BIOME_REGISTRY, () -> {
        return BiomeRegistry.PLAINS;
    });
    public static final IRegistry<GeneratorSettingBase> NOISE_GENERATOR_SETTINGS = registerSimple(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, GeneratorSettingBase::bootstrap);

    private static <T> IRegistry<T> registerSimple(ResourceKey<? extends IRegistry<T>> registryRef, Supplier<T> defaultValueSupplier) {
        return registerSimple(registryRef, Lifecycle.stable(), defaultValueSupplier);
    }

    private static <T> IRegistry<T> registerSimple(ResourceKey<? extends IRegistry<T>> registryRef, Lifecycle lifecycle, Supplier<T> defaultValueSupplier) {
        return internalRegister(registryRef, new RegistryMaterials<>(registryRef, lifecycle), defaultValueSupplier, lifecycle);
    }

    private static <T, R extends IRegistryWritable<T>> R internalRegister(ResourceKey<? extends IRegistry<T>> registryRef, R registry, Supplier<T> defaultValueSupplier, Lifecycle lifecycle) {
        MinecraftKey resourceLocation = registryRef.location();
        LOADERS.put(resourceLocation, defaultValueSupplier);
        IRegistryWritable<R> writableRegistry = WRITABLE_REGISTRY;
        return writableRegistry.register(registryRef, registry, lifecycle);
    }

    public static <T> T register(IRegistry<? super T> registry, String id, T object) {
        return register(registry, new MinecraftKey(id), object);
    }

    public static <V, T extends V> T register(IRegistry<V> registry, MinecraftKey id, T object) {
        return ((IRegistryWritable)registry).register(ResourceKey.create(registry.key(), id), object, Lifecycle.stable());
    }

    public static <V, T extends V> T registerMapping(IRegistry<V> registry, int rawId, ResourceKey<V> key, T object) {
        return ((IRegistryWritable)registry).registerMapping(rawId, key, object, Lifecycle.stable());
    }

    public static void bootstrap() {
    }

    static {
        LOADERS.forEach((resourceLocation, supplier) -> {
            if (supplier.get() == null) {
                LOGGER.error("Unable to bootstrap registry '{}'", (Object)resourceLocation);
            }

        });
        IRegistry.checkRegistry(WRITABLE_REGISTRY);
    }
}
