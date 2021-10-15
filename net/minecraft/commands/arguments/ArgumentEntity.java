package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;

public class ArgumentEntity implements ArgumentType<EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType(new ChatMessage("argument.entity.toomany"));
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(new ChatMessage("argument.player.toomany"));
    public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(new ChatMessage("argument.player.entities"));
    public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(new ChatMessage("argument.entity.notfound.entity"));
    public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(new ChatMessage("argument.entity.notfound.player"));
    public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(new ChatMessage("argument.entity.selector.not_allowed"));
    private static final byte FLAG_SINGLE = 1;
    private static final byte FLAG_PLAYERS_ONLY = 2;
    final boolean single;
    final boolean playersOnly;

    protected ArgumentEntity(boolean singleTarget, boolean playersOnly) {
        this.single = singleTarget;
        this.playersOnly = playersOnly;
    }

    public static ArgumentEntity entity() {
        return new ArgumentEntity(true, false);
    }

    public static Entity getEntity(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).findSingleEntity(context.getSource());
    }

    public static ArgumentEntity multipleEntities() {
        return new ArgumentEntity(false, false);
    }

    public static Collection<? extends Entity> getEntities(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        Collection<? extends Entity> collection = getOptionalEntities(context, name);
        if (collection.isEmpty()) {
            throw NO_ENTITIES_FOUND.create();
        } else {
            return collection;
        }
    }

    public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        return commandContext.getArgument(string, EntitySelector.class).getEntities(commandContext.getSource());
    }

    public static Collection<EntityPlayer> getOptionalPlayers(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        return commandContext.getArgument(string, EntitySelector.class).findPlayers(commandContext.getSource());
    }

    public static ArgumentEntity player() {
        return new ArgumentEntity(true, true);
    }

    public static EntityPlayer getPlayer(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        return commandContext.getArgument(string, EntitySelector.class).findSinglePlayer(commandContext.getSource());
    }

    public static ArgumentEntity players() {
        return new ArgumentEntity(false, true);
    }

    public static Collection<EntityPlayer> getPlayers(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        List<EntityPlayer> list = commandContext.getArgument(string, EntitySelector.class).findPlayers(commandContext.getSource());
        if (list.isEmpty()) {
            throw NO_PLAYERS_FOUND.create();
        } else {
            return list;
        }
    }

    @Override
    public EntitySelector parse(StringReader stringReader) throws CommandSyntaxException {
        int i = 0;
        ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader);
        EntitySelector entitySelector = entitySelectorParser.parse();
        if (entitySelector.getMaxResults() > 1 && this.single) {
            if (this.playersOnly) {
                stringReader.setCursor(0);
                throw ERROR_NOT_SINGLE_PLAYER.createWithContext(stringReader);
            } else {
                stringReader.setCursor(0);
                throw ERROR_NOT_SINGLE_ENTITY.createWithContext(stringReader);
            }
        } else if (entitySelector.includesEntities() && this.playersOnly && !entitySelector.isSelfSelector()) {
            stringReader.setCursor(0);
            throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(stringReader);
        } else {
            return entitySelector;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        if (commandContext.getSource() instanceof ICompletionProvider) {
            StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
            stringReader.setCursor(suggestionsBuilder.getStart());
            ICompletionProvider sharedSuggestionProvider = (ICompletionProvider)commandContext.getSource();
            ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader, sharedSuggestionProvider.hasPermission(2));

            try {
                entitySelectorParser.parse();
            } catch (CommandSyntaxException var7) {
            }

            return entitySelectorParser.fillSuggestions(suggestionsBuilder, (suggestionsBuilderx) -> {
                Collection<String> collection = sharedSuggestionProvider.getOnlinePlayerNames();
                Iterable<String> iterable = (Iterable<String>)(this.playersOnly ? collection : Iterables.concat(collection, sharedSuggestionProvider.getSelectedEntities()));
                ICompletionProvider.suggest(iterable, suggestionsBuilderx);
            });
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Serializer implements ArgumentSerializer<ArgumentEntity> {
        @Override
        public void serializeToNetwork(ArgumentEntity entityArgument, PacketDataSerializer friendlyByteBuf) {
            byte b = 0;
            if (entityArgument.single) {
                b = (byte)(b | 1);
            }

            if (entityArgument.playersOnly) {
                b = (byte)(b | 2);
            }

            friendlyByteBuf.writeByte(b);
        }

        @Override
        public ArgumentEntity deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
            byte b = friendlyByteBuf.readByte();
            return new ArgumentEntity((b & 1) != 0, (b & 2) != 0);
        }

        @Override
        public void serializeToJson(ArgumentEntity entityArgument, JsonObject jsonObject) {
            jsonObject.addProperty("amount", entityArgument.single ? "single" : "multiple");
            jsonObject.addProperty("type", entityArgument.playersOnly ? "players" : "entities");
        }
    }
}
