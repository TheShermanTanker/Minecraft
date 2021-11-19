package net.minecraft.data.worldgen.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugReportProviderBiome implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DebugReportGenerator generator;

    public DebugReportProviderBiome(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();

        for(Entry<ResourceKey<BiomeBase>, BiomeBase> entry : RegistryGeneration.BIOME.entrySet()) {
            Path path2 = createPath(path, entry.getKey().location());
            BiomeBase biome = entry.getValue();
            Function<Supplier<BiomeBase>, DataResult<JsonElement>> function = JsonOps.INSTANCE.withEncoder(BiomeBase.CODEC);

            try {
                Optional<JsonElement> optional = function.apply(() -> {
                    return biome;
                }).result();
                if (optional.isPresent()) {
                    DebugReportProvider.save(GSON, cache, optional.get(), path2);
                } else {
                    LOGGER.error("Couldn't serialize biome {}", (Object)path2);
                }
            } catch (IOException var9) {
                LOGGER.error("Couldn't save biome {}", path2, var9);
            }
        }

    }

    private static Path createPath(Path root, MinecraftKey id) {
        return root.resolve("reports/biomes/" + id.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Biomes";
    }
}
