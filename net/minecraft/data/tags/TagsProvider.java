package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TagsProvider<T> implements DebugReportProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    protected final DebugReportGenerator generator;
    protected final IRegistry<T> registry;
    private final Map<MinecraftKey, Tag.Builder> builders = Maps.newLinkedHashMap();

    protected TagsProvider(DebugReportGenerator root, IRegistry<T> registry) {
        this.generator = root;
        this.registry = registry;
    }

    protected abstract void addTags();

    @Override
    public void run(HashCache cache) {
        this.builders.clear();
        this.addTags();
        this.builders.forEach((id, builder) -> {
            List<Tag.BuilderEntry> list = builder.getEntries().filter((builderEntry) -> {
                return !builderEntry.getEntry().verifyIfPresent(this.registry::containsKey, this.builders::containsKey);
            }).collect(Collectors.toList());
            if (!list.isEmpty()) {
                throw new IllegalArgumentException(String.format("Couldn't define tag %s as it is missing following references: %s", id, list.stream().map(Objects::toString).collect(Collectors.joining(","))));
            } else {
                JsonObject jsonObject = builder.serializeToJson();
                Path path = this.getPath(id);

                try {
                    String string = GSON.toJson((JsonElement)jsonObject);
                    String string2 = SHA1.hashUnencodedChars(string).toString();
                    if (!Objects.equals(cache.getHash(path), string2) || !Files.exists(path)) {
                        Files.createDirectories(path.getParent());
                        BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

                        try {
                            bufferedWriter.write(string);
                        } catch (Throwable var13) {
                            if (bufferedWriter != null) {
                                try {
                                    bufferedWriter.close();
                                } catch (Throwable var12) {
                                    var13.addSuppressed(var12);
                                }
                            }

                            throw var13;
                        }

                        if (bufferedWriter != null) {
                            bufferedWriter.close();
                        }
                    }

                    cache.putNew(path, string2);
                } catch (IOException var14) {
                    LOGGER.error("Couldn't save tags to {}", path, var14);
                }

            }
        });
    }

    protected abstract Path getPath(MinecraftKey id);

    protected TagsProvider.TagAppender<T> tag(Tag.Named<T> tag) {
        Tag.Builder builder = this.getOrCreateRawBuilder(tag);
        return new TagsProvider.TagAppender<>(builder, this.registry, "vanilla");
    }

    protected Tag.Builder getOrCreateRawBuilder(Tag.Named<T> tag) {
        return this.builders.computeIfAbsent(tag.getName(), (id) -> {
            return new Tag.Builder();
        });
    }

    protected static class TagAppender<T> {
        private final Tag.Builder builder;
        private final IRegistry<T> registry;
        private final String source;

        TagAppender(Tag.Builder builder, IRegistry<T> registry, String source) {
            this.builder = builder;
            this.registry = registry;
            this.source = source;
        }

        public TagsProvider.TagAppender<T> add(T element) {
            this.builder.addElement(this.registry.getKey(element), this.source);
            return this;
        }

        public TagsProvider.TagAppender<T> addOptional(MinecraftKey id) {
            this.builder.addOptionalElement(id, this.source);
            return this;
        }

        public TagsProvider.TagAppender<T> addTag(Tag.Named<T> identifiedTag) {
            this.builder.addTag(identifiedTag.getName(), this.source);
            return this;
        }

        public TagsProvider.TagAppender<T> addOptionalTag(MinecraftKey id) {
            this.builder.addOptionalTag(id, this.source);
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(T... elements) {
            Stream.<T>of(elements).map(this.registry::getKey).forEach((id) -> {
                this.builder.addElement(id, this.source);
            });
            return this;
        }
    }
}
