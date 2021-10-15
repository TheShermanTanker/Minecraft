package net.minecraft.server.packs.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.profiling.GameProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ResourceDataJson extends ResourceDataAbstract<Map<MinecraftKey, JsonElement>> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PATH_SUFFIX = ".json";
    private static final int PATH_SUFFIX_LENGTH = ".json".length();
    private final Gson gson;
    private final String directory;

    public ResourceDataJson(Gson gson, String dataType) {
        this.gson = gson;
        this.directory = dataType;
    }

    @Override
    protected Map<MinecraftKey, JsonElement> prepare(IResourceManager resourceManager, GameProfilerFiller profilerFiller) {
        Map<MinecraftKey, JsonElement> map = Maps.newHashMap();
        int i = this.directory.length() + 1;

        for(MinecraftKey resourceLocation : resourceManager.listResources(this.directory, (path) -> {
            return path.endsWith(".json");
        })) {
            String string = resourceLocation.getKey();
            MinecraftKey resourceLocation2 = new MinecraftKey(resourceLocation.getNamespace(), string.substring(i, string.length() - PATH_SUFFIX_LENGTH));

            try {
                IResource resource = resourceManager.getResource(resourceLocation);

                try {
                    InputStream inputStream = resource.getInputStream();

                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                        try {
                            JsonElement jsonElement = ChatDeserializer.fromJson(this.gson, reader, JsonElement.class);
                            if (jsonElement != null) {
                                JsonElement jsonElement2 = map.put(resourceLocation2, jsonElement);
                                if (jsonElement2 != null) {
                                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                                }
                            } else {
                                LOGGER.error("Couldn't load data file {} from {} as it's null or empty", resourceLocation2, resourceLocation);
                            }
                        } catch (Throwable var17) {
                            try {
                                reader.close();
                            } catch (Throwable var16) {
                                var17.addSuppressed(var16);
                            }

                            throw var17;
                        }

                        reader.close();
                    } catch (Throwable var18) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable var15) {
                                var18.addSuppressed(var15);
                            }
                        }

                        throw var18;
                    }

                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Throwable var19) {
                    if (resource != null) {
                        try {
                            resource.close();
                        } catch (Throwable var14) {
                            var19.addSuppressed(var14);
                        }
                    }

                    throw var19;
                }

                if (resource != null) {
                    resource.close();
                }
            } catch (IllegalArgumentException | IOException | JsonParseException var20) {
                LOGGER.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, var20);
            }
        }

        return map;
    }
}
