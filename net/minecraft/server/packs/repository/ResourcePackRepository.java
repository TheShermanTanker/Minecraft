package net.minecraft.server.packs.repository;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.IResourcePack;

public class ResourcePackRepository implements AutoCloseable {
    private final Set<ResourcePackSource> sources;
    private Map<String, ResourcePackLoader> available = ImmutableMap.of();
    private List<ResourcePackLoader> selected = ImmutableList.of();
    private final ResourcePackLoader.PackConstructor constructor;

    public ResourcePackRepository(ResourcePackLoader.PackConstructor profileFactory, ResourcePackSource... providers) {
        this.constructor = profileFactory;
        this.sources = ImmutableSet.copyOf(providers);
    }

    public ResourcePackRepository(EnumResourcePackType type, ResourcePackSource... providers) {
        this((name, displayName, alwaysEnabled, packFactory, metadata, direction, source) -> {
            return new ResourcePackLoader(name, displayName, alwaysEnabled, packFactory, metadata, type, direction, source);
        }, providers);
    }

    public void reload() {
        List<String> list = this.selected.stream().map(ResourcePackLoader::getId).collect(ImmutableList.toImmutableList());
        this.close();
        this.available = this.discoverAvailable();
        this.selected = this.rebuildSelected(list);
    }

    private Map<String, ResourcePackLoader> discoverAvailable() {
        Map<String, ResourcePackLoader> map = Maps.newTreeMap();

        for(ResourcePackSource repositorySource : this.sources) {
            repositorySource.loadPacks((profile) -> {
                map.put(profile.getId(), profile);
            }, this.constructor);
        }

        return ImmutableMap.copyOf(map);
    }

    public void setSelected(Collection<String> enabled) {
        this.selected = this.rebuildSelected(enabled);
    }

    private List<ResourcePackLoader> rebuildSelected(Collection<String> enabledNames) {
        List<ResourcePackLoader> list = this.getAvailablePacks(enabledNames).collect(Collectors.toList());

        for(ResourcePackLoader pack : this.available.values()) {
            if (pack.isRequired() && !list.contains(pack)) {
                pack.getDefaultPosition().insert(list, pack, Functions.identity(), false);
            }
        }

        return ImmutableList.copyOf(list);
    }

    private Stream<ResourcePackLoader> getAvailablePacks(Collection<String> names) {
        return names.stream().map(this.available::get).filter(Objects::nonNull);
    }

    public Collection<String> getAvailableIds() {
        return this.available.keySet();
    }

    public Collection<ResourcePackLoader> getAvailablePacks() {
        return this.available.values();
    }

    public Collection<String> getSelectedIds() {
        return this.selected.stream().map(ResourcePackLoader::getId).collect(ImmutableSet.toImmutableSet());
    }

    public Collection<ResourcePackLoader> getSelectedPacks() {
        return this.selected;
    }

    @Nullable
    public ResourcePackLoader getPack(String name) {
        return this.available.get(name);
    }

    @Override
    public void close() {
        this.available.values().forEach(ResourcePackLoader::close);
    }

    public boolean isAvailable(String name) {
        return this.available.containsKey(name);
    }

    public List<IResourcePack> openAllSelected() {
        return this.selected.stream().map(ResourcePackLoader::open).collect(ImmutableList.toImmutableList());
    }
}
