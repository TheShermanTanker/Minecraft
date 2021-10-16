package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;

public class TagStatic {
    private static final Set<ResourceKey<?>> HELPERS_IDS = Sets.newHashSet();
    private static final List<TagUtil<?>> HELPERS = Lists.newArrayList();

    public static <T> TagUtil<T> create(ResourceKey<? extends IRegistry<T>> registryKey, String dataType) {
        if (!HELPERS_IDS.add(registryKey)) {
            throw new IllegalStateException("Duplicate entry for static tag collection: " + registryKey);
        } else {
            TagUtil<T> staticTagHelper = new TagUtil<>(registryKey, dataType);
            HELPERS.add(staticTagHelper);
            return staticTagHelper;
        }
    }

    public static void resetAll(ITagRegistry tagManager) {
        HELPERS.forEach((list) -> {
            list.reset(tagManager);
        });
    }

    public static void resetAllToEmpty() {
        HELPERS.forEach(TagUtil::resetToEmpty);
    }

    public static Multimap<ResourceKey<? extends IRegistry<?>>, MinecraftKey> getAllMissingTags(ITagRegistry tagManager) {
        Multimap<ResourceKey<? extends IRegistry<?>>, MinecraftKey> multimap = HashMultimap.create();
        HELPERS.forEach((list) -> {
            multimap.putAll(list.getKey(), list.getMissingTags(tagManager));
        });
        return multimap;
    }

    public static void bootStrap() {
        makeSureAllKnownHelpersAreLoaded();
    }

    private static Set<TagUtil<?>> getAllKnownHelpers() {
        return ImmutableSet.of(TagsBlock.HELPER, TagsItem.HELPER, TagsFluid.HELPER, TagsEntity.HELPER, TagsGameEvent.HELPER);
    }

    private static void makeSureAllKnownHelpersAreLoaded() {
        Set<ResourceKey<?>> set = getAllKnownHelpers().stream().map(TagUtil::getKey).collect(Collectors.toSet());
        if (!Sets.difference(HELPERS_IDS, set).isEmpty()) {
            throw new IllegalStateException("Missing helper registrations");
        }
    }

    public static void visitHelpers(Consumer<TagUtil<?>> consumer) {
        HELPERS.forEach(consumer);
    }

    public static ITagRegistry createCollection() {
        ITagRegistry.Builder builder = new ITagRegistry.Builder();
        makeSureAllKnownHelpersAreLoaded();
        HELPERS.forEach((list) -> {
            list.addToCollection(builder);
        });
        return builder.build();
    }
}
