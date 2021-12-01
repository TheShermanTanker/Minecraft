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

    static <T> void filterResources(Iterable<T> candidates, String remaining, Function<T, MinecraftKey> identifier, Consumer<T> action) {
        boolean bl = remaining.indexOf(58) > -1;

        for(T object : candidates) {
            MinecraftKey resourceLocation = identifier.apply(object);
            if (bl) {
                String string = resourceLocation.toString();
                if (matchesSubStr(remaining, string)) {
                    action.accept(object);
                }
            } else if (matchesSubStr(remaining, resourceLocation.getNamespace()) || resourceLocation.getNamespace().equals("minecraft") && matchesSubStr(remaining, resourceLocation.getKey())) {
                action.accept(object);
            }
        }

    }

    static <T> void filterResources(Iterable<T> candidates, String remaining, String prefix, Function<T, MinecraftKey> identifier, Consumer<T> action) {
        if (remaining.isEmpty()) {
            candidates.forEach(action);
        } else {
            String string = Strings.commonPrefix(remaining, prefix);
            if (!string.isEmpty()) {
                String string2 = remaining.substring(string.length());
                filterResources(candidates, string2, identifier, action);
            }
        }

    }

    static CompletableFuture<Suggestions> suggestResource(Iterable<MinecraftKey> candidates, SuggestionsBuilder builder, String prefix) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(candidates, string, prefix, (id) -> {
            return id;
        }, (id) -> {
            builder.suggest(prefix + id);
        });
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestResource(Iterable<MinecraftKey> candidates, SuggestionsBuilder builder) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(candidates, string, (id) -> {
            return id;
        }, (id) -> {
            builder.suggest(id.toString());
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

    static CompletableFuture<Suggestions> suggestResource(Stream<MinecraftKey> candidates, SuggestionsBuilder builder) {
        return suggestResource(candidates::iterator, builder);
    }

    static <T> CompletableFuture<Suggestions> suggestResource(Stream<T> candidates, SuggestionsBuilder builder, Function<T, MinecraftKey> identifier, Function<T, Message> tooltip) {
        return suggestResource(candidates::iterator, builder, identifier, tooltip);
    }

    static CompletableFuture<Suggestions> suggestCoordinates(String remaining, Collection<ICompletionProvider.TextCoordinates> candidates, SuggestionsBuilder builder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(remaining)) {
            for(ICompletionProvider.TextCoordinates textCoordinates : candidates) {
                String string = textCoordinates.x + " " + textCoordinates.y + " " + textCoordinates.z;
                if (predicate.test(string)) {
                    list.add(textCoordinates.x);
                    list.add(textCoordinates.x + " " + textCoordinates.y);
                    list.add(string);
                }
            }
        } else {
            String[] strings = remaining.split(" ");
            if (strings.length == 1) {
                for(ICompletionProvider.TextCoordinates textCoordinates2 : candidates) {
                    String string2 = strings[0] + " " + textCoordinates2.y + " " + textCoordinates2.z;
                    if (predicate.test(string2)) {
                        list.add(strings[0] + " " + textCoordinates2.y);
                        list.add(string2);
                    }
                }
            } else if (strings.length == 2) {
                for(ICompletionProvider.TextCoordinates textCoordinates3 : candidates) {
                    String string3 = strings[0] + " " + strings[1] + " " + textCoordinates3.z;
                    if (predicate.test(string3)) {
                        list.add(string3);
                    }
                }
            }
        }

        return suggest(list, builder);
    }

    static CompletableFuture<Suggestions> suggest2DCoordinates(String remaining, Collection<ICompletionProvider.TextCoordinates> candidates, SuggestionsBuilder builder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(remaining)) {
            for(ICompletionProvider.TextCoordinates textCoordinates : candidates) {
                String string = textCoordinates.x + " " + textCoordinates.z;
                if (predicate.test(string)) {
                    list.add(textCoordinates.x);
                    list.add(string);
                }
            }
        } else {
            String[] strings = remaining.split(" ");
            if (strings.length == 1) {
                for(ICompletionProvider.TextCoordinates textCoordinates2 : candidates) {
                    String string2 = strings[0] + " " + textCoordinates2.z;
                    if (predicate.test(string2)) {
                        list.add(string2);
                    }
                }
            }
        }

        return suggest(list, builder);
    }

    static CompletableFuture<Suggestions> suggest(Iterable<String> candidates, SuggestionsBuilder builder) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : candidates) {
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                builder.suggest(string2);
            }
        }

        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(Stream<String> candidates, SuggestionsBuilder builder) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        candidates.filter((candidate) -> {
            return matchesSubStr(string, candidate.toLowerCase(Locale.ROOT));
        }).forEach(builder::suggest);
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(String[] candidates, SuggestionsBuilder builder) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : candidates) {
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                builder.suggest(string2);
            }
        }

        return builder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> suggest(Iterable<T> candidates, SuggestionsBuilder builder, Function<T, String> suggestionText, Function<T, Message> tooltip) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);

        for(T object : candidates) {
            String string2 = suggestionText.apply(object);
            if (matchesSubStr(string, string2.toLowerCase(Locale.ROOT))) {
                builder.suggest(string2, tooltip.apply(object));
            }
        }

        return builder.buildFuture();
    }

    static boolean matchesSubStr(String remaining, String candidate) {
        for(int i = 0; !candidate.startsWith(remaining, i); ++i) {
            i = candidate.indexOf(95, i);
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
