package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldgenRegistryDumpReport implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DebugReportGenerator generator;

    public WorldgenRegistryDumpReport(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        IRegistryCustom registryAccess = IRegistryCustom.builtin();
        int i = 0;
        RegistryMaterials<WorldDimension> mappedRegistry = DimensionManager.defaultDimensions(registryAccess, 0L, false);
        ChunkGenerator chunkGenerator = GeneratorSettings.makeDefaultOverworld(registryAccess, 0L, false);
        RegistryMaterials<WorldDimension> mappedRegistry2 = GeneratorSettings.withOverworld(registryAccess.ownedRegistryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY), mappedRegistry, chunkGenerator);
        DynamicOps<JsonElement> dynamicOps = RegistryWriteOps.create(JsonOps.INSTANCE, registryAccess);
        IRegistryCustom.knownRegistries().forEach((info) -> {
            dumpRegistryCap(cache, path, registryAccess, dynamicOps, info);
        });
        dumpRegistry(path, cache, dynamicOps, IRegistry.LEVEL_STEM_REGISTRY, mappedRegistry2, WorldDimension.CODEC);
    }

    private static <T> void dumpRegistryCap(HashCache cache, Path path, IRegistryCustom registryManager, DynamicOps<JsonElement> json, IRegistryCustom.RegistryData<T> info) {
        dumpRegistry(path, cache, json, info.key(), registryManager.ownedRegistryOrThrow(info.key()), info.codec());
    }

    private static <E, T extends IRegistry<E>> void dumpRegistry(Path path, HashCache cache, DynamicOps<JsonElement> json, ResourceKey<? extends T> registryKey, T registry, Encoder<E> encoder) {
        for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
            Path path2 = createPath(path, registryKey.location(), entry.getKey().location());
            dumpValue(path2, cache, json, encoder, entry.getValue());
        }

    }

    private static <E> void dumpValue(Path path, HashCache cache, DynamicOps<JsonElement> json, Encoder<E> encoder, E value) {
        try {
            Optional<JsonElement> optional = encoder.encodeStart(json, value).result();
            if (optional.isPresent()) {
                DebugReportProvider.save(GSON, cache, optional.get(), path);
            } else {
                LOGGER.error("Couldn't serialize element {}", (Object)path);
            }
        } catch (IOException var6) {
            LOGGER.error("Couldn't save element {}", path, var6);
        }

    }

    private static Path createPath(Path root, MinecraftKey rootId, MinecraftKey id) {
        return resolveTopPath(root).resolve(id.getNamespace()).resolve(rootId.getKey()).resolve(id.getKey() + ".json");
    }

    private static Path resolveTopPath(Path path) {
        return path.resolve("reports").resolve("worldgen");
    }

    @Override
    public String getName() {
        return "Worldgen";
    }
}
