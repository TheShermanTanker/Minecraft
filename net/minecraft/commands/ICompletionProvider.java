package net.minecraft.commands;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.World;

public interface ICompletionProvider {
    Collection<String> getOnlinePlayerNames();

    default Collection<String> getSelectedEntities() {
        return Collections.emptyList();
    }

    Collection<String> getAllTeams();

    Collection<MinecraftKey> getAvailableSoundEvents();

    Stream<MinecraftKey> getRecipeNames();

    CompletableFuture<Suggestions> customSuggestion(CommandContext<ICompletionProvider> context, SuggestionsBuilder builder);

    default Collection<ICompletionProvider.TextCoordinates> getRelevantCoordinates() {
        return Collections.singleton(ICompletionProvider.TextCoordinates.DEFAULT_GLOBAL);
    }

    default Collection<ICompletionProvider.TextCoordinates> getAbsoluteCoordinates() {
        return Collections.singleton(ICompletionProvider.TextCoordinates.DEFAULT_GLOBAL);
    }

    Set<ResourceKey<World>> levels();

    IRegistryCustom registryAccess();

    boolean hasPermission(int level);

    static <T> void filterResources(Iterable<T> candidates, String string, Function<T, MinecraftKey> identifier, Consumer<T> action) {
        boolean bl = string.indexOf(58) > -1;

        for(T object : candidates) {
            MinecraftKey resourceLocation = identifier.apply(object);
            if (bl) {
                String string2 = resourceLocation.toString();
                if (matchesSubStr(string, string2)) {
                    action.accept(object);
                }
            } else if (matchesSubStr(string, resourceLocation.getNamespace()) || resourceLocation.getNamespace().equals("minecraft") && matchesSubStr(string, resourceLocation.getKey())) {
                action.accept(object);
            }
        }

    }

    static <T> void filterResources(Iterable<T> candidates, String string, String string2, Function<T, MinecraftKey> identifier, Consumer<T> action) {
        if (string.isEmpty()) {
            candidates.forEach(action);
        } else {
            String string3 = Strings.commonPrefix(string, string2);
            if (!string3.isEmpty()) {
                String string4 = string.substring(string3.length());
                filterResources(candidates, string4, identifier, action);
            }
        }

    }

    static CompletableFuture<Suggestions> suggestResource(Iterable<MinecraftKey> candidates, SuggestionsBuilder builder, String string) {
        String string2 = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(candidates, string2, string, (resourceLocation) -> {
            return resourceLocation;
        }, (resourceLocation) -> {
            builder.suggest(string + resourceLocation);
        });
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestResource(Iterable<MinecraftKey> candidates, SuggestionsBuilder builder) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(candidates, string, (resourceLocation) -> {
            return resourceLocation;
        }, (resourceLocation) -> {
            builder.suggest(resourceLocation.toString());
        });
        return builder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> suggestResource(Iterable<T> candidates, SuggestionsBuilder builder, Function<T, MinecraftKey> identifier, Function<T, Message> tooltip) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(candidates, string, identifier, (object) -> {
            builder.suggest(identifier.apply(object).toString(), tooltip.apply(object));
        });
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestResource(Stream<MinecraftKey> stream, SuggestionsBuilder builder) {
        return suggestResource(stream::iterator, builder);
    }

    static <T> CompletableFuture<Suggestions> suggestResource(Stream<T> candidates, SuggestionsBuilder builder, Function<T, MinecraftKey> identifier, Function<T, Message> tooltip) {
        return suggestResource(candidates::iterator, builder, identifier, tooltip);
    }

    static CompletableFuture<Suggestions> suggestCoordinates(String string, Collection<ICompletionProvider.TextCoordinates> candidates, SuggestionsBuilder builder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(string)) {
            for(ICompletionProvider.TextCoordinates textCoordinates : candidates) {
                String string2 = textCoordinates.x + " " + textCoordinates.y + " " + textCoordinates.z;
                if (predicate.test(string2)) {
                    list.add(textCoordinates.x);
                    list.add(textCoordinates.x + " " + textCoordinates.y);
                    list.add(string2);
                }
            }
        } else {
            String[] strings = string.split(" ");
            if (strings.length == 1) {
                for(ICompletionProvider.TextCoordinates textCoordinates2 : candidates) {
                    String string3 = strings[0] + " " + textCoordinates2.y + " " + textCoordinates2.z;
                    if (predicate.test(string3)) {
                        list.add(strings[0] + " " + textCoordinates2.y);
                        list.add(string3);
                    }
                }
            } else if (strings.length == 2) {
                for(ICompletionProvider.TextCoordinates textCoordinates3 : candidates) {
                    String string4 = strings[0] + " " + strings[1] + " " + textCoordinates3.z;
                    if (predicate.test(string4)) {
                        list.add(string4);
                    }
                }
            }
        }

        return suggest(list, builder);
    }

    static CompletableFuture<Suggestions> suggest2DCoordinates(String string, Collection<ICompletionProvider.TextCoordinates> collection, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(string)) {
            for(ICompletionProvider.TextCoordinates textCoordinates : collection) {
                String string2 = textCoordinates.x + " " + textCoordinates.z;
                if (predicate.test(string2)) {
                    list.add(textCoordinates.x);
                    list.add(string2);
                }
            }
        } else {
            String[] strings = string.split(" ");
            if (strings.length == 1) {
                for(ICompletionProvider.TextCoordinates textCoordinates2 : collection) {
                    String string3 = strings[0] + " " + textCoordinates2.z;
                    if (predicate.test(string3)) {
                        list.add(string3);
                    }
                }
            }
        }

        return suggest(list, suggestionsBuilder);
    }

    static CompletableFuture<Suggestions> suggest(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : iterable) {
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(Stream<String> stream, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        stream.filter((string2) -> {
            return matchesSubStr(string, string2.toLowerCase(Locale.ROOT));
        }).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(String[] strings, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : strings) {
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> suggest(Iterable<T> iterable, SuggestionsBuilder suggestionsBuilder, Function<T, String> function, Function<T, Message> function2) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(T object : iterable) {
            String string2 = function.apply(object);
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                suggestionsBuilder.suggest(string2, function2.apply(object));
            }
        }

        return suggestionsBuilder.buildFuture();
    }

    static boolean matchesSubStr(String string, String string2) {
        for(int i = 0; !string2.startsWith(string, i); ++i) {
            i = string2.indexOf(95, i);
            if (i < 0) {
                return false;
            }
        }

        return true;
    }

    public static class TextCoordinates {
        public static final ICompletionProvider.TextCoordinates DEFAULT_LOCAL = new ICompletionProvider.TextCoordinates("^", "^", "^");
        public static final ICompletionProvider.TextCoordinates DEFAULT_GLOBAL = new ICompletionProvider.TextCoordinates("~", "~", "~");
        public final String x;
        public final String y;
        public final String z;

        public TextCoordinates(String x, String y, String z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
