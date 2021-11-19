package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.effect.MobEffectBase;

public class ArgumentMobEffect implements ArgumentType<MobEffectBase> {
    private static final Collection<String> EXAMPLES = Arrays.asList("spooky", "effect");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_EFFECT = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("effect.effectNotFound", object);
    });

    public static ArgumentMobEffect effect() {
        return new ArgumentMobEffect();
    }

    public static MobEffectBase getEffect(CommandContext<CommandListenerWrapper> commandContext, String string) {
        return commandContext.getArgument(string, MobEffectBase.class);
    }

    @Override
    public MobEffectBase parse(StringReader stringReader) throws CommandSyntaxException {
        MinecraftKey resourceLocation = MinecraftKey.read(stringReader);
        return IRegistry.MOB_EFFECT.getOptional(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_EFFECT.create(resourceLocation);
        });
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggestResource(IRegistry.MOB_EFFECT.keySet(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
