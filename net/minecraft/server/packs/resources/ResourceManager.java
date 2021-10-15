package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.IResourcePack;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceManager implements IReloadableResourceManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ResourceManagerFallback> namespacedPacks = Maps.newHashMap();
    private final List<IReloadListener> listeners = Lists.newArrayList();
    private final Set<String> namespaces = Sets.newLinkedHashSet();
    private final List<IResourcePack> packs = Lists.newArrayList();
    private final EnumResourcePackType type;

    public ResourceManager(EnumResourcePackType type) {
        this.type = type;
    }

    public void add(IResourcePack pack) {
        this.packs.add(pack);

        for(String string : pack.getNamespaces(this.type)) {
            this.namespaces.add(string);
            ResourceManagerFallback fallbackResourceManager = this.namespacedPacks.get(string);
            if (fallbackResourceManager == null) {
                fallbackResourceManager = new ResourceManagerFallback(this.type, string);
                this.namespacedPacks.put(string, fallbackResourceManager);
            }

            fallbackResourceManager.add(pack);
        }

    }

    @Override
    public Set<String> getNamespaces() {
        return this.namespaces;
    }

    @Override
    public IResource getResource(MinecraftKey id) throws IOException {
        IResourceManager resourceManager = this.namespacedPacks.get(id.getNamespace());
        if (resourceManager != null) {
            return resourceManager.getResource(id);
        } else {
            throw new FileNotFoundException(id.toString());
        }
    }

    @Override
    public boolean hasResource(MinecraftKey id) {
        IResourceManager resourceManager = this.namespacedPacks.get(id.getNamespace());
        return resourceManager != null ? resourceManager.hasResource(id) : false;
    }

    @Override
    public List<IResource> getResources(MinecraftKey id) throws IOException {
        IResourceManager resourceManager = this.namespacedPacks.get(id.getNamespace());
        if (resourceManager != null) {
            return resourceManager.getResources(id);
        } else {
            throw new FileNotFoundException(id.toString());
        }
    }

    @Override
    public Collection<MinecraftKey> listResources(String startingPath, Predicate<String> pathPredicate) {
        Set<MinecraftKey> set = Sets.newHashSet();

        for(ResourceManagerFallback fallbackResourceManager : this.namespacedPacks.values()) {
            set.addAll(fallbackResourceManager.listResources(startingPath, pathPredicate));
        }

        List<MinecraftKey> list = Lists.newArrayList(set);
        Collections.sort(list);
        return list;
    }

    private void clear() {
        this.namespacedPacks.clear();
        this.namespaces.clear();
        this.packs.forEach(IResourcePack::close);
        this.packs.clear();
    }

    @Override
    public void close() {
        this.clear();
    }

    @Override
    public void registerReloadListener(IReloadListener reloader) {
        this.listeners.add(reloader);
    }

    @Override
    public IReloadable createReload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<IResourcePack> packs) {
        LOGGER.info("Reloading ResourceManager: {}", () -> {
            return packs.stream().map(IResourcePack::getName).collect(Collectors.joining(", "));
        });
        this.clear();

        for(IResourcePack packResources : packs) {
            try {
                this.add(packResources);
            } catch (Exception var8) {
                LOGGER.error("Failed to add resource pack {}", packResources.getName(), var8);
                return new ResourceManager.FailingReloadInstance(new ResourceManager.ResourcePackLoadingFailure(packResources, var8));
            }
        }

        return (IReloadable)(LOGGER.isDebugEnabled() ? new ReloadableProfiled(this, Lists.newArrayList(this.listeners), prepareExecutor, applyExecutor, initialStage) : Reloadable.of(this, Lists.newArrayList(this.listeners), prepareExecutor, applyExecutor, initialStage));
    }

    @Override
    public Stream<IResourcePack> listPacks() {
        return this.packs.stream();
    }

    static class FailingReloadInstance implements IReloadable {
        private final ResourceManager.ResourcePackLoadingFailure exception;
        private final CompletableFuture<Unit> failedFuture;

        public FailingReloadInstance(ResourceManager.ResourcePackLoadingFailure exception) {
            this.exception = exception;
            this.failedFuture = new CompletableFuture<>();
            this.failedFuture.completeExceptionally(exception);
        }

        @Override
        public CompletableFuture<Unit> done() {
            return this.failedFuture;
        }

        @Override
        public float getActualProgress() {
            return 0.0F;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public void checkExceptions() {
            throw this.exception;
        }
    }

    public static class ResourcePackLoadingFailure extends RuntimeException {
        private final IResourcePack pack;

        public ResourcePackLoadingFailure(IResourcePack pack, Throwable cause) {
            super(pack.getName(), cause);
            this.pack = pack;
        }

        public IResourcePack getPack() {
            return this.pack;
        }
    }
}
