package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;

public class ArgumentScoreholder implements ArgumentType<ArgumentScoreholder.Result> {
    public static final SuggestionProvider<CommandListenerWrapper> SUGGEST_SCORE_HOLDERS = (context, builder) -> {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader);

        try {
            entitySelectorParser.parse();
        } catch (CommandSyntaxException var5) {
        }

        return entitySelectorParser.fillSuggestions(builder, (builderx) -> {
            ICompletionProvider.suggest(context.getSource().getOnlinePlayerNames(), builderx);
        });
    };
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
    private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(new ChatMessage("argument.scoreHolder.empty"));
    private static final byte FLAG_MULTIPLE = 1;
    final boolean multiple;

    public ArgumentScoreholder(boolean multiple) {
        this.multiple = multiple;
    }

    public static String getName(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return getNames(context, name).iterator().next();
    }

    public static Collection<String> getNames(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return getNames(context, name, Collections::emptyList);
    }

    public static Collection<String> getNamesWithDefaultWildcard(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return getNames(context, name, context.getSource().getServer().getScoreboard()::getPlayers);
    }

    public static Collection<String> getNames(CommandContext<CommandListenerWrapper> context, String name, Supplier<Collection<String>> players) throws CommandSyntaxException {
        Collection<String> collection = context.getArgument(name, ArgumentScoreholder.Result.class).getNames(context.getSource(), players);
        if (collection.isEmpty()) {
            throw ArgumentEntity.NO_ENTITIES_FOUND.create();
        } else {
            return collection;
        }
    }

    public static ArgumentScoreholder scoreHolder() {
        return new ArgumentScoreholder(false);
    }

    public static ArgumentScoreholder scoreHolders() {
        return new ArgumentScoreholder(true);
    }

    public ArgumentScoreholder.Result parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '@') {
            ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader);
            EntitySelector entitySelector = entitySelectorParser.parse();
            if (!this.multiple && entitySelector.getMaxResults() > 1) {
                throw ArgumentEntity.ERROR_NOT_SINGLE_ENTITY.create();
            } else {
                return new ArgumentScoreholder.SelectorResult(entitySelector);
            }
        } else {
            int i = stringReader.getCursor();

            while(stringReader.canRead() && stringReader.peek() != ' ') {
                stringReader.skip();
            }

            String string = stringReader.getString().substring(i, stringReader.getCursor());
            if (string.equals("*")) {
                return (source, players) -> {
                    Collection<String> collection = players.get();
                    if (collection.isEmpty()) {
                        throw ERROR_NO_RESULTS.create();
                    } else {
                        return collection;
                    }
                };
            } else {
                Collection<String> collection = Collections.singleton(string);
                return (source, players) -> {
                    return collection;
                };
            }
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public interface Result {
        Collection<String> getNames(CommandListenerWrapper source, Supplier<Collection<String>> players) throws CommandSyntaxException;
    }

    public static class SelectorResult implements ArgumentScoreholder.Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector selector) {
            this.selector = selector;
        }

        @Override
        public Collection<String> getNames(CommandListenerWrapper source, Supplier<Collection<String>> players) throws CommandSyntaxException {
            List<? extends Entity> list = this.selector.getEntities(source);
            if (list.isEmpty()) {
                throw ArgumentEntity.NO_ENTITIES_FOUND.create();
            } else {
                List<String> list2 = Lists.newArrayList();

                for(Entity entity : list) {
                    list2.add(entity.getName());
                }

                return list2;
            }
        }
    }

    public static class Serializer implements ArgumentSerializer<ArgumentScoreholder> {
        @Override
        public void serializeToNetwork(ArgumentScoreholder type, PacketDataSerializer buf) {
            byte b = 0;
            if (type.multiple) {
                b = (byte)(b | 1);
            }

            buf.writeByte(b);
        }

        @Override
        public ArgumentScoreholder deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
            byte b = friendlyByteBuf.readByte();
            boolean bl = (b & 1) != 0;
            return new ArgumentScoreholder(bl);
        }

        @Override
        public void serializeToJson(ArgumentScoreholder type, JsonObject json) {
            json.addProperty("amount", type.multiple ? "multiple" : "single");
        }
    }
}
