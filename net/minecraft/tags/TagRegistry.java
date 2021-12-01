package net.minecraft.tags;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.IReloadListener;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.util.profiling.GameProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagRegistry implements IReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final IRegistryCustom registryAccess;
    private ITagRegistry tags = ITagRegistry.EMPTY;

    public TagRegistry(IRegistryCustom registryManager) {
        this.registryAccess = registryManager;
    }

    public ITagRegistry getTags() {
        return this.tags;
    }

    @Override
    public CompletableFuture<Void> reload(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, GameProfilerFiller prepareProfiler, GameProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        List<TagRegistry.LoaderInfo<?>> list = Lists.newArrayList();
        TagStatic.visitHelpers((requiredTagList) -> {
            TagRegistry.LoaderInfo<?> loaderInfo = this.createLoader(manager, prepareExecutor, requiredTagList);
            if (loaderInfo != null) {
                list.add(loaderInfo);
            }

        });
        return CompletableFuture.allOf(list.stream().map((requiredGroup) -> {
            return requiredGroup.pendingLoad;
        }).toArray((i) -> {
            return new CompletableFuture[i];
        })).thenCompose(synchronizer::wait).thenAcceptAsync((void_) -> {
            ITagRegistry.Builder builder = new ITagRegistry.Builder();
            list.forEach((requiredGroup) -> {
                requiredGroup.addToBuilder(builder);
            });
            ITagRegistry tagContainer = builder.build();
            Multimap<ResourceKey<? extends IRegistry<?>>, MinecraftKey> multimap = TagStatic.getAllMissingTags(tagContainer);
            if (!multimap.isEmpty()) {
                throw new IllegalStateException("Missing required tags: " + (String)multimap.entries().stream().map((entry) -> {
                    return entry.getKey() + ":" + entry.getValue();
                }).sorted().collect(Collectors.joining(",")));
            } else {
                TagsInstance.bind(tagContainer);
                this.tags = tagContainer;
            }
        }, applyExecutor);
    }

    @Nullable
    private <T> TagRegistry.LoaderInfo<T> createLoader(IResourceManager resourceManager, Executor prepareExecutor, TagUtil<T> requirement) {
        Optional<? extends IRegistry<T>> optional = this.registryAccess.registry(requirement.getKey());
        if (optional.isPresent()) {
            IRegistry<T> registry = optional.get();
            TagDataPack<T> tagLoader = new TagDataPack<>(registry::getOptional, requirement.getDirectory());
            CompletableFuture<? extends Tags<T>> completableFuture = CompletableFuture.supplyAsync(() -> {
                return tagLoader.loadAndBuild(resourceManager);
            }, prepareExecutor);
            return new TagRegistry.LoaderInfo<>(requirement, completableFuture);
        } else {
            LOGGER.warn("Can't find registry for {}", (Object)requirement.getKey());
            return null;
        }
    }

    static class LoaderInfo<T> {
        private final TagUtil<T> helper;
        final CompletableFuture<? extends Tags<T>> pendingLoad;

        LoaderInfo(TagUtil<T> requirement, CompletableFuture<? extends Tags<T>> groupLoadFuture) {
            this.helper = requirement;
            this.pendingLoad = groupLoadFuture;
        }

        public void addToBuilder(ITagRegistry.Builder builder) {
            builder.add(this.helper.getKey(), this.pendingLoad.join());
        }
    }
}
