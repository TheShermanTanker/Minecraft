package net.minecraft.tags;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;

public interface Tags<T> {
    Map<MinecraftKey, Tag<T>> getAllTags();

    @Nullable
    default Tag<T> getTag(MinecraftKey id) {
        return this.getAllTags().get(id);
    }

    Tag<T> getTagOrEmpty(MinecraftKey id);

    @Nullable
    default MinecraftKey getId(Tag.Named<T> tag) {
        return tag.getName();
    }

    @Nullable
    MinecraftKey getId(Tag<T> tag);

    default boolean hasTag(MinecraftKey id) {
        return this.getAllTags().containsKey(id);
    }

    default Collection<MinecraftKey> getAvailableTags() {
        return this.getAllTags().keySet();
    }

    default Collection<MinecraftKey> getMatchingTags(T object) {
        List<MinecraftKey> list = Lists.newArrayList();

        for(Entry<MinecraftKey, Tag<T>> entry : this.getAllTags().entrySet()) {
            if (entry.getValue().isTagged(object)) {
                list.add(entry.getKey());
            }
        }

        return list;
    }

    default Tags.NetworkPayload serializeToNetwork(IRegistry<T> registry) {
        Map<MinecraftKey, Tag<T>> map = this.getAllTags();
        Map<MinecraftKey, IntList> map2 = Maps.newHashMapWithExpectedSize(map.size());
        map.forEach((id, tag) -> {
            List<T> list = tag.getTagged();
            IntList intList = new IntArrayList(list.size());

            for(T object : list) {
                intList.add(registry.getId(object));
            }

            map2.put(id, intList);
        });
        return new Tags.NetworkPayload(map2);
    }

    static <T> Tags<T> createFromNetwork(Tags.NetworkPayload serialized, IRegistry<? extends T> registry) {
        Map<MinecraftKey, Tag<T>> map = Maps.newHashMapWithExpectedSize(serialized.tags.size());
        serialized.tags.forEach((id, entries) -> {
            Builder<T> builder = ImmutableSet.builder();

            for(int i : entries) {
                builder.add(registry.fromId(i));
            }

            map.put(id, Tag.fromSet(builder.build()));
        });
        return of(map);
    }

    static <T> Tags<T> empty() {
        return of(ImmutableBiMap.of());
    }

    static <T> Tags<T> of(Map<MinecraftKey, Tag<T>> tags) {
        final BiMap<MinecraftKey, Tag<T>> biMap = ImmutableBiMap.copyOf(tags);
        return new Tags<T>() {
            private final Tag<T> empty = TagSet.empty();

            @Override
            public Tag<T> getTagOrEmpty(MinecraftKey id) {
                return biMap.getOrDefault(id, this.empty);
            }

            @Nullable
            @Override
            public MinecraftKey getId(Tag<T> tag) {
                return tag instanceof Tag.Named ? ((Tag.Named)tag).getName() : biMap.inverse().get(tag);
            }

            @Override
            public Map<MinecraftKey, Tag<T>> getAllTags() {
                return biMap;
            }
        };
    }

    public static class NetworkPayload {
        final Map<MinecraftKey, IntList> tags;

        NetworkPayload(Map<MinecraftKey, IntList> contents) {
            this.tags = contents;
        }

        public void write(PacketDataSerializer buf) {
            buf.writeMap(this.tags, PacketDataSerializer::writeResourceLocation, PacketDataSerializer::writeIntIdList);
        }

        public static Tags.NetworkPayload read(PacketDataSerializer buf) {
            return new Tags.NetworkPayload(buf.readMap(PacketDataSerializer::readResourceLocation, PacketDataSerializer::readIntIdList));
        }
    }
}
