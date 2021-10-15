package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.CustomFunctionData;

public class CustomFunction {
    private final CustomFunction.Entry[] entries;
    final MinecraftKey id;

    public CustomFunction(MinecraftKey id, CustomFunction.Entry[] elements) {
        this.id = id;
        this.entries = elements;
    }

    public MinecraftKey getId() {
        return this.id;
    }

    public CustomFunction.Entry[] getEntries() {
        return this.entries;
    }

    public static CustomFunction fromLines(MinecraftKey id, com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> dispatcher, CommandListenerWrapper source, List<String> lines) {
        List<CustomFunction.Entry> list = Lists.newArrayListWithCapacity(lines.size());

        for(int i = 0; i < lines.size(); ++i) {
            int j = i + 1;
            String string = lines.get(i).trim();
            StringReader stringReader = new StringReader(string);
            if (stringReader.canRead() && stringReader.peek() != '#') {
                if (stringReader.peek() == '/') {
                    stringReader.skip();
                    if (stringReader.peek() == '/') {
                        throw new IllegalArgumentException("Unknown or invalid command '" + string + "' on line " + j + " (if you intended to make a comment, use '#' not '//')");
                    }

                    String string2 = stringReader.readUnquotedString();
                    throw new IllegalArgumentException("Unknown or invalid command '" + string + "' on line " + j + " (did you mean '" + string2 + "'? Do not use a preceding forwards slash.)");
                }

                try {
                    ParseResults<CommandListenerWrapper> parseResults = dispatcher.parse(stringReader, source);
                    if (parseResults.getReader().canRead()) {
                        throw CommandDispatcher.getParseException(parseResults);
                    }

                    list.add(new CustomFunction.CommandEntry(parseResults));
                } catch (CommandSyntaxException var10) {
                    throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + var10.getMessage());
                }
            }
        }

        return new CustomFunction(id, list.toArray(new CustomFunction.Entry[0]));
    }

    public static class CacheableFunction {
        public static final CustomFunction.CacheableFunction NONE = new CustomFunction.CacheableFunction((MinecraftKey)null);
        @Nullable
        private final MinecraftKey id;
        private boolean resolved;
        private Optional<CustomFunction> function = Optional.empty();

        public CacheableFunction(@Nullable MinecraftKey id) {
            this.id = id;
        }

        public CacheableFunction(CustomFunction function) {
            this.resolved = true;
            this.id = null;
            this.function = Optional.of(function);
        }

        public Optional<CustomFunction> get(CustomFunctionData manager) {
            if (!this.resolved) {
                if (this.id != null) {
                    this.function = manager.get(this.id);
                }

                this.resolved = true;
            }

            return this.function;
        }

        @Nullable
        public MinecraftKey getId() {
            return this.function.map((f) -> {
                return f.id;
            }).orElse(this.id);
        }
    }

    public static class CommandEntry implements CustomFunction.Entry {
        private final ParseResults<CommandListenerWrapper> parse;

        public CommandEntry(ParseResults<CommandListenerWrapper> parsed) {
            this.parse = parsed;
        }

        @Override
        public void execute(CustomFunctionData manager, CommandListenerWrapper source, Deque<CustomFunctionData.QueuedCommand> entries, int maxChainLength, int depth, @Nullable CustomFunctionData.TraceCallbacks tracer) throws CommandSyntaxException {
            if (tracer != null) {
                String string = this.parse.getReader().getString();
                tracer.onCommand(depth, string);
                int i = this.execute(manager, source);
                tracer.onReturn(depth, string, i);
            } else {
                this.execute(manager, source);
            }

        }

        private int execute(CustomFunctionData manager, CommandListenerWrapper source) throws CommandSyntaxException {
            return manager.getCommandDispatcher().execute(new ParseResults<>(this.parse.getContext().withSource(source), this.parse.getReader(), this.parse.getExceptions()));
        }

        @Override
        public String toString() {
            return this.parse.getReader().getString();
        }
    }

    @FunctionalInterface
    public interface Entry {
        void execute(CustomFunctionData manager, CommandListenerWrapper source, Deque<CustomFunctionData.QueuedCommand> entries, int maxChainLength, int depth, @Nullable CustomFunctionData.TraceCallbacks tracer) throws CommandSyntaxException;
    }

    public static class FunctionEntry implements CustomFunction.Entry {
        private final CustomFunction.CacheableFunction function;

        public FunctionEntry(CustomFunction function) {
            this.function = new CustomFunction.CacheableFunction(function);
        }

        @Override
        public void execute(CustomFunctionData manager, CommandListenerWrapper source, Deque<CustomFunctionData.QueuedCommand> entries, int maxChainLength, int depth, @Nullable CustomFunctionData.TraceCallbacks tracer) {
            SystemUtils.ifElse(this.function.get(manager), (f) -> {
                CustomFunction.Entry[] entrys = f.getEntries();
                if (tracer != null) {
                    tracer.onCall(depth, f.getId(), entrys.length);
                }

                int k = maxChainLength - entries.size();
                int l = Math.min(entrys.length, k);

                for(int m = l - 1; m >= 0; --m) {
                    entries.addFirst(new CustomFunctionData.QueuedCommand(source, depth + 1, entrys[m]));
                }

            }, () -> {
                if (tracer != null) {
                    tracer.onCall(depth, this.function.getId(), -1);
                }

            });
        }

        @Override
        public String toString() {
            return "function " + this.function.getId();
        }
    }
}
