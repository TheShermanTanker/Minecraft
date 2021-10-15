package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.World;

public class ArgumentDimension implements ArgumentType<MinecraftKey> {
    private static final Collection<String> EXAMPLES = Stream.of(World.OVERWORLD, World.NETHER).map((resourceKey) -> {
        return resourceKey.location().toString();
    }).collect(Collectors.toList());
    private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.dimension.invalid", object);
    });

    @Override
    public MinecraftKey parse(StringReader stringReader) throws CommandSyntaxException {
        return MinecraftKey.read(stringReader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return commandContext.getSource() instanceof ICompletionProvider ? ICompletionProvider.suggestResource(((ICompletionProvider)commandContext.getSource()).levels().stream().map(ResourceKey::location), suggestionsBuilder) : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static ArgumentDimension dimension() {
        return new ArgumentDimension();
    }

    public static WorldServer getDimension(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        MinecraftKey resourceLocation = context.getArgument(name, MinecraftKey.class);
        ResourceKey<World> resourceKey = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, resourceLocation);
        WorldServer serverLevel = context.getSource().getServer().getWorldServer(resourceKey);
        if (serverLevel == null) {
            throw ERROR_INVALID_VALUE.create(resourceLocation);
        } else {
            return serverLevel;
        }
    }
}
