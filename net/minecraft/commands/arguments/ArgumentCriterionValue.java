package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.commands.CommandListenerWrapper;

public interface ArgumentCriterionValue<T extends CriterionConditionValue<?>> extends ArgumentType<T> {
    static ArgumentCriterionValue.Ints intRange() {
        return new ArgumentCriterionValue.Ints();
    }

    static ArgumentCriterionValue.Floats floatRange() {
        return new ArgumentCriterionValue.Floats();
    }

    public static class Floats implements ArgumentCriterionValue<CriterionConditionValue.DoubleRange> {
        private static final Collection<String> EXAMPLES = Arrays.asList("0..5.2", "0", "-5.4", "-100.76..", "..100");

        public static CriterionConditionValue.DoubleRange getRange(CommandContext<CommandListenerWrapper> context, String name) {
            return context.getArgument(name, CriterionConditionValue.DoubleRange.class);
        }

        public CriterionConditionValue.DoubleRange parse(StringReader stringReader) throws CommandSyntaxException {
            return CriterionConditionValue.DoubleRange.fromReader(stringReader);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }

    public static class Ints implements ArgumentCriterionValue<CriterionConditionValue.IntegerRange> {
        private static final Collection<String> EXAMPLES = Arrays.asList("0..5", "0", "-5", "-100..", "..100");

        public static CriterionConditionValue.IntegerRange getRange(CommandContext<CommandListenerWrapper> context, String name) {
            return context.getArgument(name, CriterionConditionValue.IntegerRange.class);
        }

        public CriterionConditionValue.IntegerRange parse(StringReader stringReader) throws CommandSyntaxException {
            return CriterionConditionValue.IntegerRange.fromReader(stringReader);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}
