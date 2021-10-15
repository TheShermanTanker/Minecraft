package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.metadata.ResourcePackMetaParser;

public interface IResourcePack extends AutoCloseable {
    String METADATA_EXTENSION = ".mcmeta";
    String PACK_META = "pack.mcmeta";

    @Nullable
    InputStream getRootResource(String fileName) throws IOException;

    InputStream getResource(EnumResourcePackType type, MinecraftKey id) throws IOException;

    Collection<MinecraftKey> getResources(EnumResourcePackType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter);

    boolean hasResource(EnumResourcePackType type, MinecraftKey id);

    Set<String> getNamespaces(EnumResourcePackType type);

    @Nullable
    <T> T getMetadataSection(ResourcePackMetaParser<T> metaReader) throws IOException;

    String getName();

    @Override
    void close();
}
