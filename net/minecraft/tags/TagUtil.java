package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;

public class TagUtil<T> {
    private final ResourceKey<? extends IRegistry<T>> key;
    private final String directory;
    private Tags<T> source = Tags.empty();
    private final List<TagUtil.Wrapper<T>> wrappers = Lists.newArrayList();

    public TagUtil(ResourceKey<? extends IRegistry<T>> registryKey, String dataType) {
        this.key = registryKey;
        this.directory = dataType;
    }

    public Tag.Named<T> bind(String id) {
        TagUtil.Wrapper<T> wrapper = new TagUtil.Wrapper<>(new MinecraftKey(id));
        this.wrappers.add(wrapper);
        return wrapper;
    }

    public void resetToEmpty() {
        this.source = Tags.empty();
        Tag<T> tag = TagSet.empty();
        this.wrappers.forEach((tagx) -> {
            tagx.rebind((id) -> {
                return tag;
            });
        });
    }

    public void reset(ITagRegistry tagManager) {
        Tags<T> tagCollection = tagManager.getOrEmpty(this.key);
        this.source = tagCollection;
        this.wrappers.forEach((tag) -> {
            tag.rebind(tagCollection::getTag);
        });
    }

    public Tags<T> getAllTags() {
        return this.source;
    }

    public Set<MinecraftKey> getMissingTags(ITagRegistry tagManager) {
        Tags<T> tagCollection = tagManager.getOrEmpty(this.key);
        Set<MinecraftKey> set = this.wrappers.stream().map(TagUtil.Wrapper::getName).collect(Collectors.toSet());
        ImmutableSet<MinecraftKey> immutableSet = ImmutableSet.copyOf(tagCollection.getAvailableTags());
        return Sets.difference(set, immutableSet);
    }

    public ResourceKey<? extends IRegistry<T>> getKey() {
        return this.key;
    }

    public String getDirectory() {
        return this.directory;
    }

    protected void addToCollection(ITagRegistry.Builder manager) {
        manager.add(this.key, Tags.of(this.wrappers.stream().collect(Collectors.toMap(Tag.Named::getName, (wrapper) -> {
            return wrapper;
        }))));
    }

    static class Wrapper<T> implements Tag.Named<T> {
        @Nullable
        private Tag<T> tag;
        protected final MinecraftKey name;

        Wrapper(MinecraftKey id) {
            this.name = id;
        }

        @Override
        public MinecraftKey getName() {
            return this.name;
        }

        private Tag<T> resolve() {
            if (this.tag == null) {
                throw new IllegalStateException("Tag " + this.name + " used before it was bound");
            } else {
                return this.tag;
            }
        }

        void rebind(Function<MinecraftKey, Tag<T>> tagFactory) {
            this.tag = tagFactory.apply(this.name);
        }

        @Override
        public boolean isTagged(T entry) {
            return this.resolve().isTagged(entry);
        }

        @Override
        public List<T> getTagged() {
            return this.resolve().getTagged();
        }
    }
}
