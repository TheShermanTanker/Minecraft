package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;
import net.minecraft.world.scores.ScoreboardScore;

public class ArgumentMathOperation implements ArgumentType<ArgumentMathOperation.Operation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
    private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(new ChatMessage("arguments.operation.invalid"));
    private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(new ChatMessage("arguments.operation.div0"));

    public static ArgumentMathOperation operation() {
        return new ArgumentMathOperation();
    }

    public static ArgumentMathOperation.Operation getOperation(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, ArgumentMathOperation.Operation.class);
    }

    public ArgumentMathOperation.Operation parse(StringReader stringReader) throws CommandSyntaxException {
        if (!stringReader.canRead()) {
            throw ERROR_INVALID_OPERATION.create();
        } else {
            int i = stringReader.getCursor();

            while(stringReader.canRead() && stringReader.peek() != ' ') {
                stringReader.skip();
            }

            return getOperation(stringReader.getString().substring(i, stringReader.getCursor()));
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static ArgumentMathOperation.Operation getOperation(String operator) throws CommandSyntaxException {
        return (ArgumentMathOperation.Operation)(operator.equals("><") ? (a, b) -> {
            int i = a.getScore();
            a.setScore(b.getScore());
            b.setScore(i);
        } : getSimpleOperation(operator));
    }

    private static ArgumentMathOperation.SimpleOperation getSimpleOperation(String operator) throws CommandSyntaxException {
        switch(operator) {
        case "=":
            return (a, b) -> {
                return b;
            };
        case "+=":
            return (a, b) -> {
                return a + b;
            };
        case "-=":
            return (a, b) -> {
                return a - b;
            };
        case "*=":
            return (a, b) -> {
                return a * b;
            };
        case "/=":
            return (a, b) -> {
                if (b == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                } else {
                    return MathHelper.intFloorDiv(a, b);
                }
            };
        case "%=":
            return (a, b) -> {
                if (b == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                } else {
                    return MathHelper.positiveModulo(a, b);
                }
            };
        case "<":
            return Math::min;
        case ">":
            return Math::max;
        default:
            throw ERROR_INVALID_OPERATION.create();
        }
    }

    @FunctionalInterface
    public interface Operation {
        void apply(ScoreboardScore a, ScoreboardScore b) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface SimpleOperation extends ArgumentMathOperation.Operation {
        int apply(int a, int b) throws CommandSyntaxException;

        @Override
        default void apply(ScoreboardScore a, ScoreboardScore b) throws CommandSyntaxException {
            a.setScore(this.apply(a.getScore(), b.getScore()));
        }
    }
}
