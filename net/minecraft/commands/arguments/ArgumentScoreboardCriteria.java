package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticWrapper;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class ArgumentScoreboardCriteria implements ArgumentType<IScoreboardCriteria> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("argument.criteria.invalid", name);
    });

    private ArgumentScoreboardCriteria() {
    }

    public static ArgumentScoreboardCriteria criteria() {
        return new ArgumentScoreboardCriteria();
    }

    public static IScoreboardCriteria getCriteria(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, IScoreboardCriteria.class);
    }

    public IScoreboardCriteria parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();

        while(stringReader.canRead() && stringReader.peek() != ' ') {
            stringReader.skip();
        }

        String string = stringReader.getString().substring(i, stringReader.getCursor());
        return IScoreboardCriteria.byName(string).orElseThrow(() -> {
            stringReader.setCursor(i);
            return ERROR_INVALID_VALUE.create(string);
        });
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        List<String> list = Lists.newArrayList(IScoreboardCriteria.getCustomCriteriaNames());

        for(StatisticWrapper<?> statType : IRegistry.STAT_TYPE) {
            for(Object object : statType.getRegistry()) {
                String string = this.getName(statType, object);
                list.add(string);
            }
        }

        return ICompletionProvider.suggest(list, suggestionsBuilder);
    }

    public <T> String getName(StatisticWrapper<T> stat, Object value) {
        return Statistic.buildName(stat, (T)value);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
