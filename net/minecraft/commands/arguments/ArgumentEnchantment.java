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
import net.minecraft.world.item.enchantment.Enchantment;

public class ArgumentEnchantment implements ArgumentType<Enchantment> {
    private static final Collection<String> EXAMPLES = Arrays.asList("unbreaking", "silk_touch");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENCHANTMENT = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("enchantment.unknown", object);
    });

    public static ArgumentEnchantment enchantment() {
        return new ArgumentEnchantment();
    }

    public static Enchantment getEnchantment(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, Enchantment.class);
    }

    @Override
    public Enchantment parse(StringReader stringReader) throws CommandSyntaxException {
        MinecraftKey resourceLocation = MinecraftKey.read(stringReader);
        return IRegistry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_ENCHANTMENT.create(resourceLocation);
        });
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggestResource(IRegistry.ENCHANTMENT.keySet(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
