package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentChatFormat;
import net.minecraft.commands.arguments.ArgumentScoreboardTeam;
import net.minecraft.commands.arguments.ArgumentScoreholder;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;

public class CommandTeam {
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(new ChatMessage("commands.team.add.duplicate"));
    private static final DynamicCommandExceptionType ERROR_TEAM_NAME_TOO_LONG = new DynamicCommandExceptionType((maxLength) -> {
        return new ChatMessage("commands.team.add.longName", maxLength);
    });
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EMPTY = new SimpleCommandExceptionType(new ChatMessage("commands.team.empty.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_NAME = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.name.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_COLOR = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.color.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.friendlyfire.alreadyEnabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.friendlyfire.alreadyDisabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.seeFriendlyInvisibles.alreadyEnabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.seeFriendlyInvisibles.alreadyDisabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.nametagVisibility.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.deathMessageVisibility.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_COLLISION_UNCHANGED = new SimpleCommandExceptionType(new ChatMessage("commands.team.option.collisionRule.unchanged"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("team").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("list").executes((context) -> {
            return listTeams(context.getSource());
        }).then(net.minecraft.commands.CommandDispatcher.argument("team", ArgumentScoreboardTeam.team()).executes((context) -> {
            return listMembers(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("team", StringArgumentType.word()).executes((context) -> {
            return createTeam(context.getSource(), StringArgumentType.getString(context, "team"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("displayName", ArgumentChatComponent.textComponent()).executes((context) -> {
            return createTeam(context.getSource(), StringArgumentType.getString(context, "team"), ArgumentChatComponent.getComponent(context, "displayName"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("team", ArgumentScoreboardTeam.team()).executes((context) -> {
            return deleteTeam(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("empty").then(net.minecraft.commands.CommandDispatcher.argument("team", ArgumentScoreboardTeam.team()).executes((context) -> {
            return emptyTeam(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("join").then(net.minecraft.commands.CommandDispatcher.argument("team", ArgumentScoreboardTeam.team()).executes((context) -> {
            return joinTeam(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), Collections.singleton(context.getSource().getEntityOrException().getName()));
        }).then(net.minecraft.commands.CommandDispatcher.argument("members", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return joinTeam(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "members"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("leave").then(net.minecraft.commands.CommandDispatcher.argument("members", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return leaveTeam(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "members"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("modify").then(net.minecraft.commands.CommandDispatcher.argument("team", ArgumentScoreboardTeam.team()).then(net.minecraft.commands.CommandDispatcher.literal("displayName").then(net.minecraft.commands.CommandDispatcher.argument("displayName", ArgumentChatComponent.textComponent()).executes((context) -> {
            return setDisplayName(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ArgumentChatComponent.getComponent(context, "displayName"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("color").then(net.minecraft.commands.CommandDispatcher.argument("value", ArgumentChatFormat.color()).executes((context) -> {
            return setColor(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ArgumentChatFormat.getColor(context, "value"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("friendlyFire").then(net.minecraft.commands.CommandDispatcher.argument("allowed", BoolArgumentType.bool()).executes((context) -> {
            return setFriendlyFire(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), BoolArgumentType.getBool(context, "allowed"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("seeFriendlyInvisibles").then(net.minecraft.commands.CommandDispatcher.argument("allowed", BoolArgumentType.bool()).executes((context) -> {
            return setFriendlySight(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), BoolArgumentType.getBool(context, "allowed"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("nametagVisibility").then(net.minecraft.commands.CommandDispatcher.literal("never").executes((context) -> {
            return setNametagVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.NEVER);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hideForOtherTeams").executes((context) -> {
            return setNametagVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OTHER_TEAMS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hideForOwnTeam").executes((context) -> {
            return setNametagVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OWN_TEAM);
        })).then(net.minecraft.commands.CommandDispatcher.literal("always").executes((context) -> {
            return setNametagVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("deathMessageVisibility").then(net.minecraft.commands.CommandDispatcher.literal("never").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.NEVER);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hideForOtherTeams").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OTHER_TEAMS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hideForOwnTeam").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OWN_TEAM);
        })).then(net.minecraft.commands.CommandDispatcher.literal("always").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("collisionRule").then(net.minecraft.commands.CommandDispatcher.literal("never").executes((context) -> {
            return setCollision(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumTeamPush.NEVER);
        })).then(net.minecraft.commands.CommandDispatcher.literal("pushOwnTeam").executes((context) -> {
            return setCollision(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumTeamPush.PUSH_OWN_TEAM);
        })).then(net.minecraft.commands.CommandDispatcher.literal("pushOtherTeams").executes((context) -> {
            return setCollision(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumTeamPush.PUSH_OTHER_TEAMS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("always").executes((context) -> {
            return setCollision(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ScoreboardTeamBase.EnumTeamPush.ALWAYS);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("prefix").then(net.minecraft.commands.CommandDispatcher.argument("prefix", ArgumentChatComponent.textComponent()).executes((context) -> {
            return setPrefix(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ArgumentChatComponent.getComponent(context, "prefix"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("suffix").then(net.minecraft.commands.CommandDispatcher.argument("suffix", ArgumentChatComponent.textComponent()).executes((context) -> {
            return setSuffix(context.getSource(), ArgumentScoreboardTeam.getTeam(context, "team"), ArgumentChatComponent.getComponent(context, "suffix"));
        }))))));
    }

    private static int leaveTeam(CommandListenerWrapper source, Collection<String> members) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : members) {
            scoreboard.removePlayerFromTeam(string);
        }

        if (members.size() == 1) {
            source.sendMessage(new ChatMessage("commands.team.leave.success.single", members.iterator().next()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.team.leave.success.multiple", members.size()), true);
        }

        return members.size();
    }

    private static int joinTeam(CommandListenerWrapper source, ScoreboardTeam team, Collection<String> members) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : members) {
            scoreboard.addPlayerToTeam(string, team);
        }

        if (members.size() == 1) {
            source.sendMessage(new ChatMessage("commands.team.join.success.single", members.iterator().next(), team.getFormattedDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.team.join.success.multiple", members.size(), team.getFormattedDisplayName()), true);
        }

        return members.size();
    }

    private static int setNametagVisibility(CommandListenerWrapper source, ScoreboardTeam team, ScoreboardTeamBase.EnumNameTagVisibility visibility) throws CommandSyntaxException {
        if (team.getNameTagVisibility() == visibility) {
            throw ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED.create();
        } else {
            team.setNameTagVisibility(visibility);
            source.sendMessage(new ChatMessage("commands.team.option.nametagVisibility.success", team.getFormattedDisplayName(), visibility.getDisplayName()), true);
            return 0;
        }
    }

    private static int setDeathMessageVisibility(CommandListenerWrapper source, ScoreboardTeam team, ScoreboardTeamBase.EnumNameTagVisibility visibility) throws CommandSyntaxException {
        if (team.getDeathMessageVisibility() == visibility) {
            throw ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED.create();
        } else {
            team.setDeathMessageVisibility(visibility);
            source.sendMessage(new ChatMessage("commands.team.option.deathMessageVisibility.success", team.getFormattedDisplayName(), visibility.getDisplayName()), true);
            return 0;
        }
    }

    private static int setCollision(CommandListenerWrapper source, ScoreboardTeam team, ScoreboardTeamBase.EnumTeamPush collisionRule) throws CommandSyntaxException {
        if (team.getCollisionRule() == collisionRule) {
            throw ERROR_TEAM_COLLISION_UNCHANGED.create();
        } else {
            team.setCollisionRule(collisionRule);
            source.sendMessage(new ChatMessage("commands.team.option.collisionRule.success", team.getFormattedDisplayName(), collisionRule.getDisplayName()), true);
            return 0;
        }
    }

    private static int setFriendlySight(CommandListenerWrapper source, ScoreboardTeam team, boolean allowed) throws CommandSyntaxException {
        if (team.canSeeFriendlyInvisibles() == allowed) {
            if (allowed) {
                throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED.create();
            } else {
                throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED.create();
            }
        } else {
            team.setCanSeeFriendlyInvisibles(allowed);
            source.sendMessage(new ChatMessage("commands.team.option.seeFriendlyInvisibles." + (allowed ? "enabled" : "disabled"), team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setFriendlyFire(CommandListenerWrapper source, ScoreboardTeam team, boolean allowed) throws CommandSyntaxException {
        if (team.allowFriendlyFire() == allowed) {
            if (allowed) {
                throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED.create();
            } else {
                throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED.create();
            }
        } else {
            team.setAllowFriendlyFire(allowed);
            source.sendMessage(new ChatMessage("commands.team.option.friendlyfire." + (allowed ? "enabled" : "disabled"), team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setDisplayName(CommandListenerWrapper source, ScoreboardTeam team, IChatBaseComponent displayName) throws CommandSyntaxException {
        if (team.getDisplayName().equals(displayName)) {
            throw ERROR_TEAM_ALREADY_NAME.create();
        } else {
            team.setDisplayName(displayName);
            source.sendMessage(new ChatMessage("commands.team.option.name.success", team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setColor(CommandListenerWrapper source, ScoreboardTeam team, EnumChatFormat color) throws CommandSyntaxException {
        if (team.getColor() == color) {
            throw ERROR_TEAM_ALREADY_COLOR.create();
        } else {
            team.setColor(color);
            source.sendMessage(new ChatMessage("commands.team.option.color.success", team.getFormattedDisplayName(), color.getName()), true);
            return 0;
        }
    }

    private static int emptyTeam(CommandListenerWrapper source, ScoreboardTeam team) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        Collection<String> collection = Lists.newArrayList(team.getPlayerNameSet());
        if (collection.isEmpty()) {
            throw ERROR_TEAM_ALREADY_EMPTY.create();
        } else {
            for(String string : collection) {
                scoreboard.removePlayerFromTeam(string, team);
            }

            source.sendMessage(new ChatMessage("commands.team.empty.success", collection.size(), team.getFormattedDisplayName()), true);
            return collection.size();
        }
    }

    private static int deleteTeam(CommandListenerWrapper source, ScoreboardTeam team) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        scoreboard.removeTeam(team);
        source.sendMessage(new ChatMessage("commands.team.remove.success", team.getFormattedDisplayName()), true);
        return scoreboard.getTeams().size();
    }

    private static int createTeam(CommandListenerWrapper source, String team) throws CommandSyntaxException {
        return createTeam(source, team, new ChatComponentText(team));
    }

    private static int createTeam(CommandListenerWrapper source, String team, IChatBaseComponent displayName) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getTeam(team) != null) {
            throw ERROR_TEAM_ALREADY_EXISTS.create();
        } else if (team.length() > 16) {
            throw ERROR_TEAM_NAME_TOO_LONG.create(16);
        } else {
            ScoreboardTeam playerTeam = scoreboard.createTeam(team);
            playerTeam.setDisplayName(displayName);
            source.sendMessage(new ChatMessage("commands.team.add.success", playerTeam.getFormattedDisplayName()), true);
            return scoreboard.getTeams().size();
        }
    }

    private static int listMembers(CommandListenerWrapper source, ScoreboardTeam team) {
        Collection<String> collection = team.getPlayerNameSet();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.team.list.members.empty", team.getFormattedDisplayName()), false);
        } else {
            source.sendMessage(new ChatMessage("commands.team.list.members.success", team.getFormattedDisplayName(), collection.size(), ChatComponentUtils.formatList(collection)), false);
        }

        return collection.size();
    }

    private static int listTeams(CommandListenerWrapper source) {
        Collection<ScoreboardTeam> collection = source.getServer().getScoreboard().getTeams();
        if (collection.isEmpty()) {
            source.sendMessage(new ChatMessage("commands.team.list.teams.empty"), false);
        } else {
            source.sendMessage(new ChatMessage("commands.team.list.teams.success", collection.size(), ChatComponentUtils.formatList(collection, ScoreboardTeam::getFormattedDisplayName)), false);
        }

        return collection.size();
    }

    private static int setPrefix(CommandListenerWrapper source, ScoreboardTeam team, IChatBaseComponent prefix) {
        team.setPrefix(prefix);
        source.sendMessage(new ChatMessage("commands.team.option.prefix.success", prefix), false);
        return 1;
    }

    private static int setSuffix(CommandListenerWrapper source, ScoreboardTeam team, IChatBaseComponent suffix) {
        team.setSuffix(suffix);
        source.sendMessage(new ChatMessage("commands.team.option.suffix.success", suffix), false);
        return 1;
    }
}
