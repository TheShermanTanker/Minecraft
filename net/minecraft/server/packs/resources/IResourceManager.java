package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.IResourcePack;

public interface IResourceManager extends ResourceProvider {
    Set<String> getNamespaces();

    boolean hasResource(MinecraftKey id);

    List<IResource> getResources(MinecraftKey id) throws IOException;

    Collection<MinecraftKey> listResources(String startingPath, Predicate<String> pathPredicate);

    Stream<IResourcePack> listPacks();

    public static enum Empty implements IResourceManager {
        INSTANCE;

        @Override
        public Set<String> getNamespaces() {
            return ImmutableSet.of();
        }

        @Override
        public IResource getResource(MinecraftKey id) throws IOException {
            throw new FileNotFoundException(id.toString());
        }

        @Override
        public boolean hasResource(MinecraftKey id) {
            return false;
        }

        @Override
        public List<IResource> getResources(MinecraftKey id) {
            return ImmutableList.of();
        }

        @Override
        public Collection<MinecraftKey> listResources(String startingPath, Predicate<String> pathPredicate) {
            return ImmutableSet.of();
        }

        @Override
        public Stream<IResourcePack> listPacks() {
            return Stream.of();
        }
    }
}
