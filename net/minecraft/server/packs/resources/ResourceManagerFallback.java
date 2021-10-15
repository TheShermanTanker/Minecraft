package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.IResourcePack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceManagerFallback implements IResourceManager {
    static final Logger LOGGER = LogManager.getLogger();
    protected final List<IResourcePack> fallbacks = Lists.newArrayList();
    private final EnumResourcePackType type;
    private final String namespace;

    public ResourceManagerFallback(EnumResourcePackType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public void add(IResourcePack pack) {
        this.fallbacks.add(pack);
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of(this.namespace);
    }

    @Override
    public IResource getResource(MinecraftKey id) throws IOException {
        this.validateLocation(id);
        IResourcePack packResources = null;
        MinecraftKey resourceLocation = getMetadataLocation(id);

        for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
            IResourcePack packResources2 = this.fallbacks.get(i);
            if (packResources == null && packResources2.hasResource(this.type, resourceLocation)) {
                packResources = packResources2;
            }

            if (packResources2.hasResource(this.type, id)) {
                InputStream inputStream = null;
                if (packResources != null) {
                    inputStream = this.getWrappedResource(resourceLocation, packResources);
                }

                return new Resource(packResources2.getName(), id, this.getWrappedResource(id, packResources2), inputStream);
            }
        }

        throw new FileNotFoundException(id.toString());
    }

    @Override
    public boolean hasResource(MinecraftKey id) {
        if (!this.isValidLocation(id)) {
            return false;
        } else {
            for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
                IResourcePack packResources = this.fallbacks.get(i);
                if (packResources.hasResource(this.type, id)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected InputStream getWrappedResource(MinecraftKey id, IResourcePack pack) throws IOException {
        InputStream inputStream = pack.getResource(this.type, id);
        return (InputStream)(LOGGER.isDebugEnabled() ? new ResourceManagerFallback.LeakedResourceWarningInputStream(inputStream, id, pack.getName()) : inputStream);
    }

    private void validateLocation(MinecraftKey id) throws IOException {
        if (!this.isValidLocation(id)) {
            throw new IOException("Invalid relative path to resource: " + id);
        }
    }

    private boolean isValidLocation(MinecraftKey id) {
        return !id.getKey().contains("..");
    }

    @Override
    public List<IResource> getResources(MinecraftKey id) throws IOException {
        this.validateLocation(id);
        List<IResource> list = Lists.newArrayList();
        MinecraftKey resourceLocation = getMetadataLocation(id);

        for(IResourcePack packResources : this.fallbacks) {
            if (packResources.hasResource(this.type, id)) {
                InputStream inputStream = packResources.hasResource(this.type, resourceLocation) ? this.getWrappedResource(resourceLocation, packResources) : null;
                list.add(new Resource(packResources.getName(), id, this.getWrappedResource(id, packResources), inputStream));
            }
        }

        if (list.isEmpty()) {
            throw new FileNotFoundException(id.toString());
        } else {
            return list;
        }
    }

    @Override
    public Collection<MinecraftKey> listResources(String startingPath, Predicate<String> pathPredicate) {
        List<MinecraftKey> list = Lists.newArrayList();

        for(IResourcePack packResources : this.fallbacks) {
            list.addAll(packResources.getResources(this.type, this.namespace, startingPath, Integer.MAX_VALUE, pathPredicate));
        }

        Collections.sort(list);
        return list;
    }

    @Override
    public Stream<IResourcePack> listPacks() {
        return this.fallbacks.stream();
    }

    static MinecraftKey getMetadataLocation(MinecraftKey id) {
        return new MinecraftKey(id.getNamespace(), id.getKey() + ".mcmeta");
    }

    static class LeakedResourceWarningInputStream extends FilterInputStream {
        private final String message;
        private boolean closed;

        public LeakedResourceWarningInputStream(InputStream parent, MinecraftKey id, String packName) {
            super(parent);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            (new Exception()).printStackTrace(new PrintStream(byteArrayOutputStream));
            this.message = "Leaked resource: '" + id + "' loaded from pack: '" + packName + "'\n" + byteArrayOutputStream;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        @Override
        protected void finalize() throws Throwable {
            if (!this.closed) {
                ResourceManagerFallback.LOGGER.warn(this.message);
            }

            super.finalize();
        }
    }
}
