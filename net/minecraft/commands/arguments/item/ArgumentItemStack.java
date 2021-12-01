package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.tags.TagsItem;

public class ArgumentItemStack implements ArgumentType<ArgumentPredicateItemStack> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");

    public static ArgumentItemStack item() {
        return new ArgumentItemStack();
    }

    public ArgumentPredicateItemStack parse(StringReader stringReader) throws CommandSyntaxException {
        ArgumentParserItemStack itemParser = (new ArgumentParserItemStack(stringReader, false)).parse();
        return new ArgumentPredicateItemStack(itemParser.getItem(), itemParser.getNbt());
    }

    public static <S> ArgumentPredicateItemStack getItem(CommandContext<S> context, String name) {
        return context.getArgument(name, ArgumentPredicateItemStack.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
        stringReader.setCursor(suggestionsBuilder.getStart());
        ArgumentParserItemStack itemParser = new ArgumentParserItemStack(stringReader, false);

        try {
            itemParser.parse();
        } catch (CommandSyntaxException var6) {
        }

        return itemParser.fillSuggestions(suggestionsBuilder, TagsItem.getAllTags());
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
