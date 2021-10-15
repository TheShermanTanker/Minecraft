package net.minecraft.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.gametest.framework.GameTestHarnessTestCommand;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.game.PacketPlayOutCommands;
import net.minecraft.server.commands.CommandAdvancement;
import net.minecraft.server.commands.CommandAttribute;
import net.minecraft.server.commands.CommandBan;
import net.minecraft.server.commands.CommandBanIp;
import net.minecraft.server.commands.CommandBanList;
import net.minecraft.server.commands.CommandBossBar;
import net.minecraft.server.commands.CommandClear;
import net.minecraft.server.commands.CommandClone;
import net.minecraft.server.commands.CommandDatapack;
import net.minecraft.server.commands.CommandDebug;
import net.minecraft.server.commands.CommandDeop;
import net.minecraft.server.commands.CommandDifficulty;
import net.minecraft.server.commands.CommandEffect;
import net.minecraft.server.commands.CommandEnchant;
import net.minecraft.server.commands.CommandExecute;
import net.minecraft.server.commands.CommandFill;
import net.minecraft.server.commands.CommandForceload;
import net.minecraft.server.commands.CommandFunction;
import net.minecraft.server.commands.CommandGamemode;
import net.minecraft.server.commands.CommandGamemodeDefault;
import net.minecraft.server.commands.CommandGamerule;
import net.minecraft.server.commands.CommandGive;
import net.minecraft.server.commands.CommandHelp;
import net.minecraft.server.commands.CommandIdleTimeout;
import net.minecraft.server.commands.CommandKick;
import net.minecraft.server.commands.CommandKill;
import net.minecraft.server.commands.CommandList;
import net.minecraft.server.commands.CommandLocate;
import net.minecraft.server.commands.CommandLocateBiome;
import net.minecraft.server.commands.CommandLoot;
import net.minecraft.server.commands.CommandMe;
import net.minecraft.server.commands.CommandOp;
import net.minecraft.server.commands.CommandPardon;
import net.minecraft.server.commands.CommandPardonIP;
import net.minecraft.server.commands.CommandParticle;
import net.minecraft.server.commands.CommandPlaySound;
import net.minecraft.server.commands.CommandPublish;
import net.minecraft.server.commands.CommandRecipe;
import net.minecraft.server.commands.CommandReload;
import net.minecraft.server.commands.CommandSaveAll;
import net.minecraft.server.commands.CommandSaveOff;
import net.minecraft.server.commands.CommandSaveOn;
import net.minecraft.server.commands.CommandSay;
import net.minecraft.server.commands.CommandSchedule;
import net.minecraft.server.commands.CommandScoreboard;
import net.minecraft.server.commands.CommandSeed;
import net.minecraft.server.commands.CommandSetBlock;
import net.minecraft.server.commands.CommandSetWorldSpawn;
import net.minecraft.server.commands.CommandSpawnpoint;
import net.minecraft.server.commands.CommandSpectate;
import net.minecraft.server.commands.CommandSpreadPlayers;
import net.minecraft.server.commands.CommandStop;
import net.minecraft.server.commands.CommandStopSound;
import net.minecraft.server.commands.CommandSummon;
import net.minecraft.server.commands.CommandTag;
import net.minecraft.server.commands.CommandTeam;
import net.minecraft.server.commands.CommandTeamMsg;
import net.minecraft.server.commands.CommandTeleport;
import net.minecraft.server.commands.CommandTell;
import net.minecraft.server.commands.CommandTellRaw;
import net.minecraft.server.commands.CommandTime;
import net.minecraft.server.commands.CommandTitle;
import net.minecraft.server.commands.CommandTrigger;
import net.minecraft.server.commands.CommandWeather;
import net.minecraft.server.commands.CommandWhitelist;
import net.minecraft.server.commands.CommandWorldBorder;
import net.minecraft.server.commands.CommandXp;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.data.CommandData;
import net.minecraft.server.level.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandDispatcher {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATORS = 1;
    public static final int LEVEL_GAMEMASTERS = 2;
    public static final int LEVEL_ADMINS = 3;
    public static final int LEVEL_OWNERS = 4;
    private final com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> dispatcher = new com.mojang.brigadier.CommandDispatcher<>();

    public CommandDispatcher(CommandDispatcher.ServerType environment) {
        CommandAdvancement.register(this.dispatcher);
        CommandAttribute.register(this.dispatcher);
        CommandExecute.register(this.dispatcher);
        CommandBossBar.register(this.dispatcher);
        CommandClear.register(this.dispatcher);
        CommandClone.register(this.dispatcher);
        CommandData.register(this.dispatcher);
        CommandDatapack.register(this.dispatcher);
        CommandDebug.register(this.dispatcher);
        CommandGamemodeDefault.register(this.dispatcher);
        CommandDifficulty.register(this.dispatcher);
        CommandEffect.register(this.dispatcher);
        CommandMe.register(this.dispatcher);
        CommandEnchant.register(this.dispatcher);
        CommandXp.register(this.dispatcher);
        CommandFill.register(this.dispatcher);
        CommandForceload.register(this.dispatcher);
        CommandFunction.register(this.dispatcher);
        CommandGamemode.register(this.dispatcher);
        CommandGamerule.register(this.dispatcher);
        CommandGive.register(this.dispatcher);
        CommandHelp.register(this.dispatcher);
        ItemCommands.register(this.dispatcher);
        CommandKick.register(this.dispatcher);
        CommandKill.register(this.dispatcher);
        CommandList.register(this.dispatcher);
        CommandLocate.register(this.dispatcher);
        CommandLocateBiome.register(this.dispatcher);
        CommandLoot.register(this.dispatcher);
        CommandTell.register(this.dispatcher);
        CommandParticle.register(this.dispatcher);
        CommandPlaySound.register(this.dispatcher);
        CommandReload.register(this.dispatcher);
        CommandRecipe.register(this.dispatcher);
        CommandSay.register(this.dispatcher);
        CommandSchedule.register(this.dispatcher);
        CommandScoreboard.register(this.dispatcher);
        CommandSeed.register(this.dispatcher, environment != CommandDispatcher.ServerType.INTEGRATED);
        CommandSetBlock.register(this.dispatcher);
        CommandSpawnpoint.register(this.dispatcher);
        CommandSetWorldSpawn.register(this.dispatcher);
        CommandSpectate.register(this.dispatcher);
        CommandSpreadPlayers.register(this.dispatcher);
        CommandStopSound.register(this.dispatcher);
        CommandSummon.register(this.dispatcher);
        CommandTag.register(this.dispatcher);
        CommandTeam.register(this.dispatcher);
        CommandTeamMsg.register(this.dispatcher);
        CommandTeleport.register(this.dispatcher);
        CommandTellRaw.register(this.dispatcher);
        CommandTime.register(this.dispatcher);
        CommandTitle.register(this.dispatcher);
        CommandTrigger.register(this.dispatcher);
        CommandWeather.register(this.dispatcher);
        CommandWorldBorder.register(this.dispatcher);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestHarnessTestCommand.register(this.dispatcher);
        }

        if (environment.includeDedicated) {
            CommandBanIp.register(this.dispatcher);
            CommandBanList.register(this.dispatcher);
            CommandBan.register(this.dispatcher);
            CommandDeop.register(this.dispatcher);
            CommandOp.register(this.dispatcher);
            CommandPardon.register(this.dispatcher);
            CommandPardonIP.register(this.dispatcher);
            PerfCommand.register(this.dispatcher);
            CommandSaveAll.register(this.dispatcher);
            CommandSaveOff.register(this.dispatcher);
            CommandSaveOn.register(this.dispatcher);
            CommandIdleTimeout.register(this.dispatcher);
            CommandStop.register(this.dispatcher);
            CommandWhitelist.register(this.dispatcher);
        }

        if (environment.includeIntegrated) {
            CommandPublish.register(this.dispatcher);
        }

        this.dispatcher.findAmbiguities((parent, child, sibling, inputs) -> {
            LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", this.dispatcher.getPath(child), this.dispatcher.getPath(sibling), inputs);
        });
        this.dispatcher.setConsumer((context, success, result) -> {
            context.getSource().onCommandComplete(context, success, result);
        });
    }

    public int performCommand(CommandListenerWrapper commandSource, String command) {
        StringReader stringReader = new StringReader(command);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        commandSource.getServer().getMethodProfiler().enter(command);

        try {
            try {
                return this.dispatcher.execute(stringReader, commandSource);
            } catch (CommandException var13) {
                commandSource.sendFailureMessage(var13.getComponent());
                return 0;
            } catch (CommandSyntaxException var14) {
                commandSource.sendFailureMessage(ChatComponentUtils.fromMessage(var14.getRawMessage()));
                if (var14.getInput() != null && var14.getCursor() >= 0) {
                    int i = Math.min(var14.getInput().length(), var14.getCursor());
                    IChatMutableComponent mutableComponent = (new ChatComponentText("")).withStyle(EnumChatFormat.GRAY).format((style) -> {
                        return style.setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.SUGGEST_COMMAND, command));
                    });
                    if (i > 10) {
                        mutableComponent.append("...");
                    }

                    mutableComponent.append(var14.getInput().substring(Math.max(0, i - 10), i));
                    if (i < var14.getInput().length()) {
                        IChatBaseComponent component = (new ChatComponentText(var14.getInput().substring(i))).withStyle(new EnumChatFormat[]{EnumChatFormat.RED, EnumChatFormat.UNDERLINE});
                        mutableComponent.addSibling(component);
                    }

                    mutableComponent.addSibling((new ChatMessage("command.context.here")).withStyle(new EnumChatFormat[]{EnumChatFormat.RED, EnumChatFormat.ITALIC}));
                    commandSource.sendFailureMessage(mutableComponent);
                }
            } catch (Exception var15) {
                IChatMutableComponent mutableComponent2 = new ChatComponentText(var15.getMessage() == null ? var15.getClass().getName() : var15.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("Command exception: {}", command, var15);
                    StackTraceElement[] stackTraceElements = var15.getStackTrace();

                    for(int j = 0; j < Math.min(stackTraceElements.length, 3); ++j) {
                        mutableComponent2.append("\n\n").append(stackTraceElements[j].getMethodName()).append("\n ").append(stackTraceElements[j].getFileName()).append(":").append(String.valueOf(stackTraceElements[j].getLineNumber()));
                    }
                }

                commandSource.sendFailureMessage((new ChatMessage("command.failed")).format((style) -> {
                    return style.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, mutableComponent2));
                }));
                if (SharedConstants.IS_RUNNING_IN_IDE) {
                    commandSource.sendFailureMessage(new ChatComponentText(SystemUtils.describeError(var15)));
                    LOGGER.error("'{}' threw an exception", command, var15);
                }

                return 0;
            }

            return 0;
        } finally {
            commandSource.getServer().getMethodProfiler().exit();
        }
    }

    public void sendCommands(EntityPlayer player) {
        Map<CommandNode<CommandListenerWrapper>, CommandNode<ICompletionProvider>> map = Maps.newHashMap();
        RootCommandNode<ICompletionProvider> rootCommandNode = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), rootCommandNode);
        this.fillUsableCommands(this.dispatcher.getRoot(), rootCommandNode, player.getCommandListener(), map);
        player.connection.sendPacket(new PacketPlayOutCommands(rootCommandNode));
    }

    private void fillUsableCommands(CommandNode<CommandListenerWrapper> tree, CommandNode<ICompletionProvider> result, CommandListenerWrapper source, Map<CommandNode<CommandListenerWrapper>, CommandNode<ICompletionProvider>> resultNodes) {
        for(CommandNode<CommandListenerWrapper> commandNode : tree.getChildren()) {
            if (commandNode.canUse(source)) {
                ArgumentBuilder<ICompletionProvider, ?> argumentBuilder = commandNode.createBuilder();
                argumentBuilder.requires((sourcex) -> {
                    return true;
                });
                if (argumentBuilder.getCommand() != null) {
                    argumentBuilder.executes((context) -> {
                        return 0;
                    });
                }

                if (argumentBuilder instanceof RequiredArgumentBuilder) {
                    RequiredArgumentBuilder<ICompletionProvider, ?> requiredArgumentBuilder = (RequiredArgumentBuilder)argumentBuilder;
                    if (requiredArgumentBuilder.getSuggestionsProvider() != null) {
                        requiredArgumentBuilder.suggests(CompletionProviders.safelySwap(requiredArgumentBuilder.getSuggestionsProvider()));
                    }
                }

                if (argumentBuilder.getRedirect() != null) {
                    argumentBuilder.redirect(resultNodes.get(argumentBuilder.getRedirect()));
                }

                CommandNode<ICompletionProvider> commandNode2 = argumentBuilder.build();
                resultNodes.put(commandNode, commandNode2);
                result.addChild(commandNode2);
                if (!commandNode.getChildren().isEmpty()) {
                    this.fillUsableCommands(commandNode, commandNode2, source, resultNodes);
                }
            }
        }

    }

    public static LiteralArgumentBuilder<CommandListenerWrapper> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<CommandListenerWrapper, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static Predicate<String> createValidator(CommandDispatcher.ParseFunction parser) {
        return (string) -> {
            try {
                parser.parse(new StringReader(string));
                return true;
            } catch (CommandSyntaxException var3) {
                return false;
            }
        };
    }

    public com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> getDispatcher() {
        return this.dispatcher;
    }

    @Nullable
    public static <S> CommandSyntaxException getParseException(ParseResults<S> parse) {
        if (!parse.getReader().canRead()) {
            return null;
        } else if (parse.getExceptions().size() == 1) {
            return parse.getExceptions().values().iterator().next();
        } else {
            return parse.getContext().getRange().isEmpty() ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader()) : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
        }
    }

    public static void validate() {
        RootCommandNode<CommandListenerWrapper> rootCommandNode = (new CommandDispatcher(CommandDispatcher.ServerType.ALL)).getDispatcher().getRoot();
        Set<ArgumentType<?>> set = ArgumentRegistry.findUsedArgumentTypes(rootCommandNode);
        Set<ArgumentType<?>> set2 = set.stream().filter((type) -> {
            return !ArgumentRegistry.isTypeRegistered(type);
        }).collect(Collectors.toSet());
        if (!set2.isEmpty()) {
            LOGGER.warn("Missing type registration for following arguments:\n {}", set2.stream().map((type) -> {
                return "\t" + type;
            }).collect(Collectors.joining(",\n")));
            throw new IllegalStateException("Unregistered argument types");
        }
    }

    @FunctionalInterface
    public interface ParseFunction {
        void parse(StringReader reader) throws CommandSyntaxException;
    }

    public static enum ServerType {
        ALL(true, true),
        DEDICATED(false, true),
        INTEGRATED(true, false);

        final boolean includeIntegrated;
        final boolean includeDedicated;

        private ServerType(boolean integrated, boolean dedicated) {
            this.includeIntegrated = integrated;
            this.includeDedicated = dedicated;
        }
    }
}
