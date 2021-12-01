package net.minecraft.commands.synchronization;

import com.google.common.collect.Maps;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;

public class CompletionProviders {
    private static final Map<MinecraftKey, SuggestionProvider<ICompletionProvider>> PROVIDERS_BY_NAME = Maps.newHashMap();
    private static final MinecraftKey DEFAULT_NAME = new MinecraftKey("ask_server");
    public static final SuggestionProvider<ICompletionProvider> ASK_SERVER = register(DEFAULT_NAME, (context, builder) -> {
        return context.getSource().customSuggestion(context, builder);
    });
    public static final SuggestionProvider<CommandListenerWrapper> ALL_RECIPES = register(new MinecraftKey("all_recipes"), (context, builder) -> {
        return ICompletionProvider.suggestResource(context.getSource().getRecipeNames(), builder);
    });
    public static final SuggestionProvider<CommandListenerWrapper> AVAILABLE_SOUNDS = register(new MinecraftKey("available_sounds"), (context, builder) -> {
        return ICompletionProvider.suggestResource(context.getSource().getAvailableSoundEvents(), builder);
    });
    public static final SuggestionProvider<CommandListenerWrapper> AVAILABLE_BIOMES = register(new MinecraftKey("available_biomes"), (context, builder) -> {
        return ICompletionProvider.suggestResource(context.getSource().registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY).keySet(), builder);
    });
    public static final SuggestionProvider<CommandListenerWrapper> SUMMONABLE_ENTITIES = register(new MinecraftKey("summonable_entities"), (context, builder) -> {
        return ICompletionProvider.suggestResource(IRegistry.ENTITY_TYPE.stream().filter(EntityTypes::canSummon), builder, EntityTypes::getName, (entityType) -> {
            return new ChatMessage(SystemUtils.makeDescriptionId("entity", EntityTypes.getName(entityType)));
        });
    });

    public static <S extends ICompletionProvider> SuggestionProvider<S> register(MinecraftKey name, SuggestionProvider<ICompletionProvider> provider) {
        if (PROVIDERS_BY_NAME.containsKey(name)) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name " + name);
        } else {
            PROVIDERS_BY_NAME.put(name, provider);
            return new CompletionProviders.Wrapper(name, provider);
        }
    }

    public static SuggestionProvider<ICompletionProvider> getProvider(MinecraftKey id) {
        return PROVIDERS_BY_NAME.getOrDefault(id, ASK_SERVER);
    }

    public static MinecraftKey getName(SuggestionProvider<ICompletionProvider> provider) {
        return provider instanceof CompletionProviders.Wrapper ? ((CompletionProviders.Wrapper)provider).name : DEFAULT_NAME;
    }

    public static SuggestionProvider<ICompletionProvider> safelySwap(SuggestionProvider<ICompletionProvider> provider) {
        return provider instanceof CompletionProviders.Wrapper ? provider : ASK_SERVER;
    }

    protected static class Wrapper implements SuggestionProvider<ICompletionProvider> {
        private final SuggestionProvider<ICompletionProvider> delegate;
        final MinecraftKey name;

        public Wrapper(MinecraftKey name, SuggestionProvider<ICompletionProvider> provider) {
            this.delegate = provider;
            this.name = name;
        }

        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ICompletionProvider> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
            return this.delegate.getSuggestions(commandContext, suggestionsBuilder);
        }
    }
}
