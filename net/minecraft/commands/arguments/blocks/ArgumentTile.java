package net.minecraft.commands.arguments.blocks;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.tags.TagsBlock;

public class ArgumentTile implements ArgumentType<ArgumentTileLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");

    public static ArgumentTile block() {
        return new ArgumentTile();
    }

    public ArgumentTileLocation parse(StringReader stringReader) throws CommandSyntaxException {
        ArgumentBlock blockStateParser = (new ArgumentBlock(stringReader, false)).parse(true);
        return new ArgumentTileLocation(blockStateParser.getBlockData(), blockStateParser.getStateMap().keySet(), blockStateParser.getNbt());
    }

    public static ArgumentTileLocation getBlock(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, ArgumentTileLocation.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
        stringReader.setCursor(suggestionsBuilder.getStart());
        ArgumentBlock blockStateParser = new ArgumentBlock(stringReader, false);

        try {
            blockStateParser.parse(true);
        } catch (CommandSyntaxException var6) {
        }

        return blockStateParser.fillSuggestions(suggestionsBuilder, TagsBlock.getAllTags());
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
