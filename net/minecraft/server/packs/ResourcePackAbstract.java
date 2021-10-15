package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.metadata.ResourcePackMetaParser;
import net.minecraft.util.ChatDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ResourcePackAbstract implements IResourcePack {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final File file;

    public ResourcePackAbstract(File base) {
        this.file = base;
    }

    private static String getPathFromLocation(EnumResourcePackType type, MinecraftKey id) {
        return String.format("%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getKey());
    }

    protected static String getRelativePath(File base, File target) {
        return base.toURI().relativize(target.toURI()).getPath();
    }

    @Override
    public InputStream getResource(EnumResourcePackType type, MinecraftKey id) throws IOException {
        return this.getResource(getPathFromLocation(type, id));
    }

    @Override
    public boolean hasResource(EnumResourcePackType type, MinecraftKey id) {
        return this.hasResource(getPathFromLocation(type, id));
    }

    protected abstract InputStream getResource(String name) throws IOException;

    @Override
    public InputStream getRootResource(String fileName) throws IOException {
        if (!fileName.contains("/") && !fileName.contains("\\")) {
            return this.getResource(fileName);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    protected abstract boolean hasResource(String name);

    protected void logWarning(String namespace) {
        LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", namespace, this.file);
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(ResourcePackMetaParser<T> metaReader) throws IOException {
        InputStream inputStream = this.getResource("pack.mcmeta");

        Object var3;
        try {
            var3 = getMetadataFromStream(metaReader, inputStream);
        } catch (Throwable var6) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return (T)var3;
    }

    @Nullable
    public static <T> T getMetadataFromStream(ResourcePackMetaParser<T> metaReader, InputStream inputStream) {
        JsonObject jsonObject;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            try {
                jsonObject = ChatDeserializer.parse(bufferedReader);
            } catch (Throwable var8) {
                try {
                    bufferedReader.close();
                } catch (Throwable var6) {
                    var8.addSuppressed(var6);
                }

                throw var8;
            }

            bufferedReader.close();
        } catch (JsonParseException | IOException var9) {
            LOGGER.error("Couldn't load {} metadata", metaReader.getMetadataSectionName(), var9);
            return (T)null;
        }

        if (!jsonObject.has(metaReader.getMetadataSectionName())) {
            return (T)null;
        } else {
            try {
                return metaReader.fromJson(ChatDeserializer.getAsJsonObject(jsonObject, metaReader.getMetadataSectionName()));
            } catch (JsonParseException var7) {
                LOGGER.error("Couldn't load {} metadata", metaReader.getMetadataSectionName(), var7);
                return (T)null;
            }
        }
    }

    @Override
    public String getName() {
        return this.file.getName();
    }
}
