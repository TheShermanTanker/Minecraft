package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.player.EntityHuman;

public class CommandXp {
    private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID = new SimpleCommandExceptionType(new ChatMessage("commands.experience.set.points.invalid"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralCommandNode<CommandListenerWrapper> literalCommandNode = dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("experience").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("amount", IntegerArgumentType.integer()).executes((context) -> {
            return addExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.POINTS);
        }).then(net.minecraft.commands.CommandDispatcher.literal("points").executes((context) -> {
            return addExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.POINTS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("levels").executes((context) -> {
            return addExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.LEVELS);
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).then(net.minecraft.commands.CommandDispatcher.argument("amount", IntegerArgumentType.integer(0)).executes((context) -> {
            return setExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.POINTS);
        }).then(net.minecraft.commands.CommandDispatcher.literal("points").executes((context) -> {
            return setExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.POINTS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("levels").executes((context) -> {
            return setExperience(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), CommandXp.Unit.LEVELS);
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("query").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.player()).then(net.minecraft.commands.CommandDispatcher.literal("points").executes((context) -> {
            return queryExperience(context.getSource(), ArgumentEntity.getPlayer(context, "targets"), CommandXp.Unit.POINTS);
        })).then(net.minecraft.commands.CommandDispatcher.literal("levels").executes((context) -> {
            return queryExperience(context.getSource(), ArgumentEntity.getPlayer(context, "targets"), CommandXp.Unit.LEVELS);
        })))));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("xp").requires((source) -> {
            return source.hasPermission(2);
        }).redirect(literalCommandNode));
    }

    private static int queryExperience(CommandListenerWrapper source, EntityPlayer player, CommandXp.Unit component) {
        int i = component.query.applyAsInt(player);
        source.sendMessage(new ChatMessage("commands.experience.query." + component.name, player.getScoreboardDisplayName(), i), false);
        return i;
    }

    private static int addExperience(CommandListenerWrapper source, Collection<? extends EntityPlayer> targets, int amount, CommandXp.Unit component) {
        for(EntityPlayer serverPlayer : targets) {
            component.add.accept(serverPlayer, amount);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.experience.add." + component.name + ".success.single", amount, targets.iterator().next().getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.experience.add." + component.name + ".success.multiple", amount, targets.size()), true);
        }

        return targets.size();
    }

    private static int setExperience(CommandListenerWrapper source, Collection<? extends EntityPlayer> targets, int amount, CommandXp.Unit component) throws CommandSyntaxException {
        int i = 0;

        for(EntityPlayer serverPlayer : targets) {
            if (component.set.test(serverPlayer, amount)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_SET_POINTS_INVALID.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.experience.set." + component.name + ".success.single", amount, targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.experience.set." + component.name + ".success.multiple", amount, targets.size()), true);
            }

            return targets.size();
        }
    }

    static enum Unit {
        POINTS("points", EntityHuman::giveExp, (player, xp) -> {
            if (xp >= player.getExpToLevel()) {
                return false;
            } else {
                player.setExperiencePoints(xp);
                return true;
            }
        }, (player) -> {
            return MathHelper.floor(player.experienceProgress * (float)player.getExpToLevel());
        }),
        LEVELS("levels", EntityPlayer::levelDown, (player, level) -> {
            player.setExperienceLevels(level);
            return true;
        }, (player) -> {
            return player.experienceLevel;
        });

        public final BiConsumer<EntityPlayer, Integer> add;
        public final BiPredicate<EntityPlayer, Integer> set;
        public final String name;
        final ToIntFunction<EntityPlayer> query;

        private Unit(String name, BiConsumer<EntityPlayer, Integer> adder, BiPredicate<EntityPlayer, Integer> setter, ToIntFunction<EntityPlayer> getter) {
            this.add = adder;
            this.name = name;
            this.set = setter;
            this.query = getter;
        }
    }
}
