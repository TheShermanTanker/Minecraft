package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;

public interface Tag<T> {
    static <T> Codec<Tag<T>> codec(Supplier<Tags<T>> groupGetter) {
        return MinecraftKey.CODEC.flatXmap((id) -> {
            return Optional.ofNullable(groupGetter.get().getTag(id)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown tag: " + id);
            });
        }, (tag) -> {
            return Optional.ofNullable(groupGetter.get().getId(tag)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown tag: " + tag);
            });
        });
    }

    boolean isTagged(T entry);

    List<T> getTagged();

    default T getRandomElement(Random random) {
        List<T> list = this.getTagged();
        return list.get(random.nextInt(list.size()));
    }

    static <T> Tag<T> fromSet(Set<T> values) {
        return TagSet.create(values);
    }

    public static class Builder {
        private final List<Tag.BuilderEntry> entries = Lists.newArrayList();

        public static Tag.Builder tag() {
            return new Tag.Builder();
        }

        public Tag.Builder add(Tag.BuilderEntry trackedEntry) {
            this.entries.add(trackedEntry);
            return this;
        }

        public Tag.Builder add(Tag.Entry entry, String source) {
            return this.add(new Tag.BuilderEntry(entry, source));
        }

        public Tag.Builder addElement(MinecraftKey id, String source) {
            return this.add(new Tag.ElementEntry(id), source);
        }

        public Tag.Builder addOptionalElement(MinecraftKey id, String source) {
            return this.add(new Tag.OptionalElementEntry(id), source);
        }

        public Tag.Builder addTag(MinecraftKey id, String source) {
            return this.add(new Tag.TagEntry(id), source);
        }

        public Tag.Builder addOptionalTag(MinecraftKey id, String source) {
            return this.add(new Tag.OptionalTagEntry(id), source);
        }

        public <T> Either<Collection<Tag.BuilderEntry>, Tag<T>> build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter) {
            ImmutableSet.Builder<T> builder = ImmutableSet.builder();
            List<Tag.BuilderEntry> list = Lists.newArrayList();

            for(Tag.BuilderEntry builderEntry : this.entries) {
                if (!builderEntry.getEntry().build(tagGetter, objectGetter, builder::add)) {
                    list.add(builderEntry);
                }
            }

            return list.isEmpty() ? Either.right(Tag.fromSet(builder.build())) : Either.left(list);
        }

        public Stream<Tag.BuilderEntry> getEntries() {
            return this.entries.stream();
        }

        public void visitRequiredDependencies(Consumer<MinecraftKey> consumer) {
            this.entries.forEach((builderEntry) -> {
                builderEntry.entry.visitRequiredDependencies(consumer);
            });
        }

        public void visitOptionalDependencies(Consumer<MinecraftKey> consumer) {
            this.entries.forEach((builderEntry) -> {
                builderEntry.entry.visitOptionalDependencies(consumer);
            });
        }

        public Tag.Builder addFromJson(JsonObject json, String source) {
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(json, "values");
            List<Tag.Entry> list = Lists.newArrayList();

            for(JsonElement jsonElement : jsonArray) {
                list.add(parseEntry(jsonElement));
            }

            if (ChatDeserializer.getAsBoolean(json, "replace", false)) {
                this.entries.clear();
            }

            list.forEach((entry) -> {
                this.entries.add(new Tag.BuilderEntry(entry, source));
            });
            return this;
        }

        private static Tag.Entry parseEntry(JsonElement json) {
            String string;
            boolean bl;
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                string = ChatDeserializer.getAsString(jsonObject, "id");
                bl = ChatDeserializer.getAsBoolean(jsonObject, "required", true);
            } else {
                string = ChatDeserializer.convertToString(json, "id");
                bl = true;
            }

            if (string.startsWith("#")) {
                MinecraftKey resourceLocation = new MinecraftKey(string.substring(1));
                return (Tag.Entry)(bl ? new Tag.TagEntry(resourceLocation) : new Tag.OptionalTagEntry(resourceLocation));
            } else {
                MinecraftKey resourceLocation2 = new MinecraftKey(string);
                return (Tag.Entry)(bl ? new Tag.ElementEntry(resourceLocation2) : new Tag.OptionalElementEntry(resourceLocation2));
            }
        }

        public JsonObject serializeToJson() {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();

            for(Tag.BuilderEntry builderEntry : this.entries) {
                builderEntry.getEntry().serializeTo(jsonArray);
            }

            jsonObject.addProperty("replace", false);
            jsonObject.add("values", jsonArray);
            return jsonObject;
        }
    }

    public static class BuilderEntry {
        final Tag.Entry entry;
        private final String source;

        BuilderEntry(Tag.Entry entry, String source) {
            this.entry = entry;
            this.source = source;
        }

        public Tag.Entry getEntry() {
            return this.entry;
        }

        public String getSource() {
            return this.source;
        }

        @Override
        public String toString() {
            return this.entry + " (from " + this.source + ")";
        }
    }

    public static class ElementEntry implements Tag.Entry {
        private final MinecraftKey id;

        public ElementEntry(MinecraftKey id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter, Consumer<T> collector) {
            T object = objectGetter.apply(this.id);
            if (object == null) {
                return false;
            } else {
                collector.accept(object);
                return true;
            }
        }

        @Override
        public void serializeTo(JsonArray json) {
            json.add(this.id.toString());
        }

        @Override
        public boolean verifyIfPresent(Predicate<MinecraftKey> existenceTest, Predicate<MinecraftKey> duplicationTest) {
            return existenceTest.test(this.id);
        }

        @Override
        public String toString() {
            return this.id.toString();
        }
    }

    public interface Entry {
        <T> boolean build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter, Consumer<T> collector);

        void serializeTo(JsonArray json);

        default void visitRequiredDependencies(Consumer<MinecraftKey> consumer) {
        }

        default void visitOptionalDependencies(Consumer<MinecraftKey> consumer) {
        }

        boolean verifyIfPresent(Predicate<MinecraftKey> existenceTest, Predicate<MinecraftKey> duplicationTest);
    }

    public interface Named<T> extends Tag<T> {
        MinecraftKey getName();
    }

    public static class OptionalElementEntry implements Tag.Entry {
        private final MinecraftKey id;

        public OptionalElementEntry(MinecraftKey id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter, Consumer<T> collector) {
            T object = objectGetter.apply(this.id);
            if (object != null) {
                collector.accept(object);
            }

            return true;
        }

        @Override
        public void serializeTo(JsonArray json) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", this.id.toString());
            jsonObject.addProperty("required", false);
            json.add(jsonObject);
        }

        @Override
        public boolean verifyIfPresent(Predicate<MinecraftKey> existenceTest, Predicate<MinecraftKey> duplicationTest) {
            return true;
        }

        @Override
        public String toString() {
            return this.id + "?";
        }
    }

    public static class OptionalTagEntry implements Tag.Entry {
        private final MinecraftKey id;

        public OptionalTagEntry(MinecraftKey id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter, Consumer<T> collector) {
            Tag<T> tag = tagGetter.apply(this.id);
            if (tag != null) {
                tag.getTagged().forEach(collector);
            }

            return true;
        }

        @Override
        public void serializeTo(JsonArray json) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "#" + this.id);
            jsonObject.addProperty("required", false);
            json.add(jsonObject);
        }

        @Override
        public String toString() {
            return "#" + this.id + "?";
        }

        @Override
        public void visitOptionalDependencies(Consumer<MinecraftKey> consumer) {
            consumer.accept(this.id);
        }

        @Override
        public boolean verifyIfPresent(Predicate<MinecraftKey> existenceTest, Predicate<MinecraftKey> duplicationTest) {
            return true;
        }
    }

    public static class TagEntry implements Tag.Entry {
        private final MinecraftKey id;

        public TagEntry(MinecraftKey id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<MinecraftKey, Tag<T>> tagGetter, Function<MinecraftKey, T> objectGetter, Consumer<T> collector) {
            Tag<T> tag = tagGetter.apply(this.id);
            if (tag == null) {
                return false;
            } else {
                tag.getTagged().forEach(collector);
                return true;
            }
        }

        @Override
        public void serializeTo(JsonArray json) {
            json.add("#" + this.id);
        }

        @Override
        public String toString() {
            return "#" + this.id;
        }

        @Override
        public boolean verifyIfPresent(Predicate<MinecraftKey> existenceTest, Predicate<MinecraftKey> duplicationTest) {
            return duplicationTest.test(this.id);
        }

        @Override
        public void visitRequiredDependencies(Consumer<MinecraftKey> consumer) {
            consumer.accept(this.id);
        }
    }
}
