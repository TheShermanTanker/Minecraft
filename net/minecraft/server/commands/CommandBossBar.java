package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.bossevents.BossBattleCustom;
import net.minecraft.server.bossevents.BossBattleCustomData;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.BossBattle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;

public class CommandBossBar {
    private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.bossbar.create.failed", name);
    });
    private static final DynamicCommandExceptionType ERROR_DOESNT_EXIST = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.bossbar.unknown", name);
    });
    private static final SimpleCommandExceptionType ERROR_NO_PLAYER_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.players.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_NAME_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.name.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_COLOR_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.color.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_STYLE_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.style.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_VALUE_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.value.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_MAX_CHANGE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.max.unchanged"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_HIDDEN = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.visibility.unchanged.hidden"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_VISIBLE = new SimpleCommandExceptionType(new ChatMessage("commands.bossbar.set.visibility.unchanged.visible"));
    public static final SuggestionProvider<CommandListenerWrapper> SUGGEST_BOSS_BAR = (context, builder) -> {
        return ICompletionProvider.suggestResource(context.getSource().getServer().getBossBattleCustomData().getIds(), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("bossbar").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("id", ArgumentMinecraftKeyRegistered.id()).then(net.minecraft.commands.CommandDispatcher.argument("name", ArgumentChatComponent.textComponent()).executes((context) -> {
            return createBar(context.getSource(), ArgumentMinecraftKeyRegistered.getId(context, "id"), ArgumentChatComponent.getComponent(context, "name"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("id", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_BOSS_BAR).executes((context) -> {
            return removeBar(context.getSource(), getBossBar(context));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listBars(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("id", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_BOSS_BAR).then(net.minecraft.commands.CommandDispatcher.literal("name").then(net.minecraft.commands.CommandDispatcher.argument("name", ArgumentChatComponent.textComponent()).executes((context) -> {
            return setName(context.getSource(), getBossBar(context), ArgumentChatComponent.getComponent(context, "name"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("color").then(net.minecraft.commands.CommandDispatcher.literal("pink").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.PINK);
        })).then(net.minecraft.commands.CommandDispatcher.literal("blue").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.BLUE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("red").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.RED);
        })).then(net.minecraft.commands.CommandDispatcher.literal("green").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.GREEN);
        })).then(net.minecraft.commands.CommandDispatcher.literal("yellow").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.YELLOW);
        })).then(net.minecraft.commands.CommandDispatcher.literal("purple").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.PURPLE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("white").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossBattle.BarColor.WHITE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("style").then(net.minecraft.commands.CommandDispatcher.literal("progress").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossBattle.BarStyle.PROGRESS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("notched_6").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossBattle.BarStyle.NOTCHED_6);
        })).then(net.minecraft.commands.CommandDispatcher.literal("notched_10").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossBattle.BarStyle.NOTCHED_10);
        })).then(net.minecraft.commands.CommandDispatcher.literal("notched_12").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossBattle.BarStyle.NOTCHED_12);
        })).then(net.minecraft.commands.CommandDispatcher.literal("notched_20").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossBattle.BarStyle.NOTCHED_20);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("value").then(net.minecraft.commands.CommandDispatcher.argument("value", IntegerArgumentType.integer(0)).executes((context) -> {
            return setValue(context.getSource(), getBossBar(context), IntegerArgumentType.getInteger(context, "value"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("max").then(net.minecraft.commands.CommandDispatcher.argument("max", IntegerArgumentType.integer(1)).executes((context) -> {
            return setMax(context.getSource(), getBossBar(context), IntegerArgumentType.getInteger(context, "max"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("visible").then(net.minecraft.commands.CommandDispatcher.argument("visible", BoolArgumentType.bool()).executes((context) -> {
            return setVisible(context.getSource(), getBossBar(context), BoolArgumentType.getBool(context, "visible"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("players").executes((context) -> {
            return setPlayers(context.getSource(), getBossBar(context), Collections.emptyList());
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return setPlayers(context.getSource(), getBossBar(context), ArgumentEntity.getOptionalPlayers(context, "targets"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("get").then(net.minecraft.commands.CommandDispatcher.argument("id", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_BOSS_BAR).then(net.minecraft.commands.CommandDispatcher.literal("value").executes((context) -> {
            return getValue(context.getSource(), getBossBar(context));
        })).then(net.minecraft.commands.CommandDispatcher.literal("max").executes((context) -> {
            return getMax(context.getSource(), getBossBar(context));
        })).then(net.minecraft.commands.CommandDispatcher.literal("visible").executes((context) -> {
            return getVisible(context.getSource(), getBossBar(context));
        })).then(net.minecraft.commands.CommandDispatcher.literal("players").executes((context) -> {
            return getPlayers(context.getSource(), getBossBar(context));
        })))));
    }

    private static int getValue(CommandListenerWrapper source, BossBattleCustom bossBar) {
        source.sendMessage(new ChatMessage("commands.bossbar.get.value", bossBar.getDisplayName(), bossBar.getValue()), true);
        return bossBar.getValue();
    }

    private static int getMax(CommandListenerWrapper source, BossBattleCustom bossBar) {
        source.sendMessage(new ChatMessage("commands.bossbar.get.max", bossBar.getDisplayName(), bossBar.getMax()), true);
        return bossBar.getMax();
    }

    private static int getVisible(CommandListenerWrapper source, BossBattleCustom bossBar) {
        if (bossBar.isVisible()) {
            source.sendMessage(new ChatMessage("commands.bossbar.get.visible.visible", bossBar.getDisplayName()), true);
            return 1;
        } else {
            source.sendMessage(new ChatMessage("commands.bossbar.get.visible.hidden", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int getPlayers(CommandListenerWrapper source, BossBattleCustom bossBar) {
        if (bossBar.getPlayers().isEmpty()) {
            source.sendMessage(new ChatMessage("commands.bossbar.get.players.none", bossBar.getDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.bossbar.get.players.some", bossBar.getDisplayName(), bossBar.getPlayers().size(), ChatComponentUtils.formatList(bossBar.getPlayers(), EntityHuman::getScoreboardDisplayName)), true);
        }

        return bossBar.getPlayers().size();
    }

    private static int setVisible(CommandListenerWrapper source, BossBattleCustom bossBar, boolean visible) throws CommandSyntaxException {
        if (bossBar.isVisible() == visible) {
            if (visible) {
                throw ERROR_ALREADY_VISIBLE.create();
            } else {
                throw ERROR_ALREADY_HIDDEN.create();
            }
        } else {
            bossBar.setVisible(visible);
            if (visible) {
                source.sendMessage(new ChatMessage("commands.bossbar.set.visible.success.visible", bossBar.getDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.bossbar.set.visible.success.hidden", bossBar.getDisplayName()), true);
            }

            return 0;
        }
    }

    private static int setValue(CommandListenerWrapper source, BossBattleCustom bossBar, int value) throws CommandSyntaxException {
        if (bossBar.getValue() == value) {
            throw ERROR_NO_VALUE_CHANGE.create();
        } else {
            bossBar.setValue(value);
            source.sendMessage(new ChatMessage("commands.bossbar.set.value.success", bossBar.getDisplayName(), value), true);
            return value;
        }
    }

    private static int setMax(CommandListenerWrapper source, BossBattleCustom bossBar, int value) throws CommandSyntaxException {
        if (bossBar.getMax() == value) {
            throw ERROR_NO_MAX_CHANGE.create();
        } else {
            bossBar.setMax(value);
            source.sendMessage(new ChatMessage("commands.bossbar.set.max.success", bossBar.getDisplayName(), value), true);
            return value;
        }
    }

    private static int setColor(CommandListenerWrapper source, BossBattleCustom bossBar, BossBattle.BarColor color) throws CommandSyntaxException {
        if (bossBar.getColor().equals(color)) {
            throw ERROR_NO_COLOR_CHANGE.create();
        } else {
            bossBar.setColor(color);
            source.sendMessage(new ChatMessage("commands.bossbar.set.color.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setStyle(CommandListenerWrapper source, BossBattleCustom bossBar, BossBattle.BarStyle style) throws CommandSyntaxException {
        if (bossBar.getOverlay().equals(style)) {
            throw ERROR_NO_STYLE_CHANGE.create();
        } else {
            bossBar.setOverlay(style);
            source.sendMessage(new ChatMessage("commands.bossbar.set.style.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setName(CommandListenerWrapper source, BossBattleCustom bossBar, IChatBaseComponent name) throws CommandSyntaxException {
        IChatBaseComponent component = ChatComponentUtils.filterForDisplay(source, name, (Entity)null, 0);
        if (bossBar.getName().equals(component)) {
            throw ERROR_NO_NAME_CHANGE.create();
        } else {
            bossBar.setName(component);
            source.sendMessage(new ChatMessage("commands.bossbar.set.name.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setPlayers(CommandListenerWrapper source, BossBattleCustom bossBar, Collection<EntityPlayer> players) throws CommandSyntaxException {
        boolean bl = bossBar.setPlayers(players);
        if (!bl) {
            throw ERROR_NO_PLAYER_CHANGE.create();
        } else {
            if (bossBar.getPlayers().isEmpty()) {
                source.sendMessage(new ChatMessage("commands.bossbar.set.players.success.none", bossBar.getDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.bossbar.set.players.success.some", bossBar.getDisplayName(), players.size(), ChatComponentUtils.formatList(players, EntityHuman::getScoreboardDisplayName)), true);
            }

            return bossBar.getPlayers().size();
        }
    }

    private static int listBars(CommandListenerWrapper source) {
        Collection<BossBattleCustom> collection = source.getServer().getBossBattleCustomData().getBattles();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.bossbar.list.bars.none"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.bossbar.list.bars.some", collection.size(), ChatComponentUtils.formatList(collection, BossBattleCustom::getDisplayName)), false);
        }

        return collection.size();
    }

    private static int createBar(CommandListenerWrapper source, MinecraftKey name, IChatBaseComponent displayName) throws CommandSyntaxException {
        BossBattleCustomData customBossEvents = source.getServer().getBossBattleCustomData();
        if (customBossEvents.get(name) != null) {
            throw ERROR_ALREADY_EXISTS.create(name.toString());
        } else {
            BossBattleCustom customBossEvent = customBossEvents.register(name, ChatComponentUtils.filterForDisplay(source, displayName, (Entity)null, 0));
            source.sendMessage(new ChatMessage("commands.bossbar.create.success", customBossEvent.getDisplayName()), true);
            return customBossEvents.getBattles().size();
        }
    }

    private static int removeBar(CommandListenerWrapper source, BossBattleCustom bossBar) {
        BossBattleCustomData customBossEvents = source.getServer().getBossBattleCustomData();
        bossBar.removeAllPlayers();
        customBossEvents.remove(bossBar);
        source.sendMessage(new ChatMessage("commands.bossbar.remove.success", bossBar.getDisplayName()), true);
        return customBossEvents.getBattles().size();
    }

    public static BossBattleCustom getBossBar(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException {
        MinecraftKey resourceLocation = ArgumentMinecraftKeyRegistered.getId(context, "id");
        BossBattleCustom customBossEvent = context.getSource().getServer().getBossBattleCustomData().get(resourceLocation);
        if (customBossEvent == null) {
            throw ERROR_DOESNT_EXIST.create(resourceLocation.toString());
        } else {
            return customBossEvent;
        }
    }
}
