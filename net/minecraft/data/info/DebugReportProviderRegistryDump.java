package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.IRegistry;
import net.minecraft.core.RegistryBlocks;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;

public class DebugReportProviderRegistryDump implements DebugReportProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DebugReportGenerator generator;

    public DebugReportProviderRegistryDump(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        JsonObject jsonObject = new JsonObject();
        IRegistry.REGISTRY.keySet().forEach((id) -> {
            jsonObject.add(id.toString(), dumpRegistry(IRegistry.REGISTRY.get(id)));
        });
        Path path = this.generator.getOutputFolder().resolve("reports/registries.json");
        DebugReportProvider.save(GSON, cache, jsonObject, path);
    }

    private static <T> JsonElement dumpRegistry(IRegistry<T> registry) {
        JsonObject jsonObject = new JsonObject();
        if (registry instanceof RegistryBlocks) {
            MinecraftKey resourceLocation = ((RegistryBlocks)registry).getDefaultKey();
            jsonObject.addProperty("default", resourceLocation.toString());
        }

        int i = IRegistry.REGISTRY.getId(registry);
        jsonObject.addProperty("protocol_id", i);
        JsonObject jsonObject2 = new JsonObject();

        for(MinecraftKey resourceLocation2 : registry.keySet()) {
            T object = registry.get(resourceLocation2);
            int j = registry.getId(object);
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("protocol_id", j);
            jsonObject2.add(resourceLocation2.toString(), jsonObject3);
        }

        jsonObject.add("entries", jsonObject2);
        return jsonObject;
    }

    @Override
    public String getName() {
        return "Registry Dump";
    }
}
