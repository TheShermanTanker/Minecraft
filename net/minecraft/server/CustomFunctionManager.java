package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICommandListener;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.packs.resources.IReloadListener;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagDataPack;
import net.minecraft.tags.Tags;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomFunctionManager implements IReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FILE_EXTENSION = ".mcfunction";
    private static final int PATH_PREFIX_LENGTH = "functions/".length();
    private static final int PATH_SUFFIX_LENGTH = ".mcfunction".length();
    private volatile Map<MinecraftKey, CustomFunction> functions = ImmutableMap.of();
    private final TagDataPack<CustomFunction> tagsLoader = new TagDataPack<>(this::getFunction, "tags/functions");
    private volatile Tags<CustomFunction> tags = Tags.empty();
    private final int functionCompilationLevel;
    private final CommandDispatcher<CommandListenerWrapper> dispatcher;

    public Optional<CustomFunction> getFunction(MinecraftKey id) {
        return Optional.ofNullable(this.functions.get(id));
    }

    public Map<MinecraftKey, CustomFunction> getFunctions() {
        return this.functions;
    }

    public Tags<CustomFunction> getTags() {
        return this.tags;
    }

    public Tag<CustomFunction> getTag(MinecraftKey id) {
        return this.tags.getTagOrEmpty(id);
    }

    public CustomFunctionManager(int level, CommandDispatcher<CommandListenerWrapper> commandDispatcher) {
        this.functionCompilationLevel = level;
        this.dispatcher = commandDispatcher;
    }

    @Override
    public CompletableFuture<Void> reload(IReloadListener.PreparationBarrier synchronizer, IResourceManager manager, GameProfilerFiller prepareProfiler, GameProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        CompletableFuture<Map<MinecraftKey, Tag.Builder>> completableFuture = CompletableFuture.supplyAsync(() -> {
            return this.tagsLoader.load(manager);
        }, prepareExecutor);
        CompletableFuture<Map<MinecraftKey, CompletableFuture<CustomFunction>>> completableFuture2 = CompletableFuture.supplyAsync(() -> {
            return manager.listResources("functions", (path) -> {
                return path.endsWith(".mcfunction");
            });
        }, prepareExecutor).thenCompose((ids) -> {
            Map<MinecraftKey, CompletableFuture<CustomFunction>> map = Maps.newHashMap();
            CommandListenerWrapper commandSourceStack = new CommandListenerWrapper(ICommandListener.NULL, Vec3D.ZERO, Vec2F.ZERO, (WorldServer)null, this.functionCompilationLevel, "", ChatComponentText.EMPTY, (MinecraftServer)null, (Entity)null);

            for(MinecraftKey resourceLocation : ids) {
                String string = resourceLocation.getKey();
                MinecraftKey resourceLocation2 = new MinecraftKey(resourceLocation.getNamespace(), string.substring(PATH_PREFIX_LENGTH, string.length() - PATH_SUFFIX_LENGTH));
                map.put(resourceLocation2, CompletableFuture.supplyAsync(() -> {
                    List<String> list = readLines(manager, resourceLocation);
                    return CustomFunction.fromLines(resourceLocation2, this.dispatcher, commandSourceStack, list);
                }, prepareExecutor));
            }

            CompletableFuture<?>[] completableFutures = map.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(completableFutures).handle((unused, ex) -> {
                return map;
            });
        });
        return completableFuture.thenCombine(completableFuture2, Pair::of).thenCompose(synchronizer::wait).thenAcceptAsync((intermediate) -> {
            Map<MinecraftKey, CompletableFuture<CustomFunction>> map = (Map)intermediate.getSecond();
            Builder<MinecraftKey, CustomFunction> builder = ImmutableMap.builder();
            map.forEach((id, functionFuture) -> {
                functionFuture.handle((function, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to load function {}", id, ex);
                    } else {
                        builder.put(id, function);
                    }

                    return null;
                }).join();
            });
            this.functions = builder.build();
            this.tags = this.tagsLoader.build((Map)intermediate.getFirst());
        }, applyExecutor);
    }

    private static List<String> readLines(IResourceManager resourceManager, MinecraftKey id) {
        try {
            IResource resource = resourceManager.getResource(id);

            List var3;
            try {
                var3 = IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (Throwable var6) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (resource != null) {
                resource.close();
            }

            return var3;
        } catch (IOException var7) {
            throw new CompletionException(var7);
        }
    }
}
