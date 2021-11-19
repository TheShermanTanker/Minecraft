package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;

public class ArgumentProfile implements ArgumentType<ArgumentProfile.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
    public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType(new ChatMessage("argument.player.unknown"));

    public static Collection<GameProfile> getGameProfiles(CommandContext<CommandListenerWrapper> commandContext, String string) throws CommandSyntaxException {
        return commandContext.getArgument(string, ArgumentProfile.Result.class).getNames(commandContext.getSource());
    }

    public static ArgumentProfile gameProfile() {
        return new ArgumentProfile();
    }

    @Override
    public ArgumentProfile.Result parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '@') {
            ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader);
            EntitySelector entitySelector = entitySelectorParser.parse();
            if (entitySelector.includesEntities()) {
                throw ArgumentEntity.ERROR_ONLY_PLAYERS_ALLOWED.create();
            } else {
                return new ArgumentProfile.SelectorResult(entitySelector);
            }
        } else {
            int i = stringReader.getCursor();

            while(stringReader.canRead() && stringReader.peek() != ' ') {
                stringReader.skip();
            }

            String string = stringReader.getString().substring(i, stringReader.getCursor());
            return (commandSourceStack) -> {
                Optional<GameProfile> optional = commandSourceStack.getServer().getUserCache().getProfile(string);
                return Collections.singleton(optional.orElseThrow(ERROR_UNKNOWN_PLAYER::create));
            };
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        if (commandContext.getSource() instanceof ICompletionProvider) {
            StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
            stringReader.setCursor(suggestionsBuilder.getStart());
            ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(stringReader);

            try {
                entitySelectorParser.parse();
            } catch (CommandSyntaxException var6) {
            }

            return entitySelectorParser.fillSuggestions(suggestionsBuilder, (suggestionsBuilderx) -> {
                ICompletionProvider.suggest(((ICompletionProvider)commandContext.getSource()).getOnlinePlayerNames(), suggestionsBuilderx);
            });
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public interface Result {
        Collection<GameProfile> getNames(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException;
    }

    public static class SelectorResult implements ArgumentProfile.Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector entitySelector) {
            this.selector = entitySelector;
        }

        @Override
        public Collection<GameProfile> getNames(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
            List<EntityPlayer> list = this.selector.findPlayers(commandSourceStack);
            if (list.isEmpty()) {
                throw ArgumentEntity.NO_PLAYERS_FOUND.create();
            } else {
                List<GameProfile> list2 = Lists.newArrayList();

                for(EntityPlayer serverPlayer : list) {
                    list2.add(serverPlayer.getProfile());
                }

                return list2;
            }
        }
    }
}
