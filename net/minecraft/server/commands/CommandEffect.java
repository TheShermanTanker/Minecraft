package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMobEffect;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;

public class CommandEffect {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.effect.give.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.effect.clear.everything.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.effect.clear.specific.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("effect").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("clear").executes((context) -> {
            return clearEffects(context.getSource(), ImmutableList.of(context.getSource().getEntityOrException()));
        }).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).executes((context) -> {
            return clearEffects(context.getSource(), ArgumentEntity.getEntities(context, "targets"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("effect", ArgumentMobEffect.effect()).executes((context) -> {
            return clearEffect(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentMobEffect.getEffect(context, "effect"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("give").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("effect", ArgumentMobEffect.effect()).executes((context) -> {
            return giveEffect(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentMobEffect.getEffect(context, "effect"), (Integer)null, 0, true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("seconds", IntegerArgumentType.integer(1, 1000000)).executes((context) -> {
            return giveEffect(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentMobEffect.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), 0, true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("amplifier", IntegerArgumentType.integer(0, 255)).executes((context) -> {
            return giveEffect(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentMobEffect.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), IntegerArgumentType.getInteger(context, "amplifier"), true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("hideParticles", BoolArgumentType.bool()).executes((context) -> {
            return giveEffect(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentMobEffect.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), IntegerArgumentType.getInteger(context, "amplifier"), !BoolArgumentType.getBool(context, "hideParticles"));
        }))))))));
    }

    private static int giveEffect(CommandListenerWrapper source, Collection<? extends Entity> targets, MobEffectList effect, @Nullable Integer seconds, int amplifier, boolean showParticles) throws CommandSyntaxException {
        int i = 0;
        int j;
        if (seconds != null) {
            if (effect.isInstant()) {
                j = seconds;
            } else {
                j = seconds * 20;
            }
        } else if (effect.isInstant()) {
            j = 1;
        } else {
            j = 600;
        }

        for(Entity entity : targets) {
            if (entity instanceof EntityLiving) {
                MobEffect mobEffectInstance = new MobEffect(effect, j, amplifier, false, showParticles);
                if (((EntityLiving)entity).addEffect(mobEffectInstance, source.getEntity())) {
                    ++i;
                }
            }
        }

        if (i == 0) {
            throw ERROR_GIVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.effect.give.success.single", effect.getDisplayName(), targets.iterator().next().getScoreboardDisplayName(), j / 20), true);
            } else {
                source.sendMessage(new ChatMessage("commands.effect.give.success.multiple", effect.getDisplayName(), targets.size(), j / 20), true);
            }

            return i;
        }
    }

    private static int clearEffects(CommandListenerWrapper source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity instanceof EntityLiving && ((EntityLiving)entity).removeAllEffects()) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_CLEAR_EVERYTHING_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.effect.clear.everything.success.single", targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.effect.clear.everything.success.multiple", targets.size()), true);
            }

            return i;
        }
    }

    private static int clearEffect(CommandListenerWrapper source, Collection<? extends Entity> targets, MobEffectList effect) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity instanceof EntityLiving && ((EntityLiving)entity).removeEffect(effect)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_CLEAR_SPECIFIC_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendMessage(new ChatMessage("commands.effect.clear.specific.success.single", effect.getDisplayName(), targets.iterator().next().getScoreboardDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.effect.clear.specific.success.multiple", effect.getDisplayName(), targets.size()), true);
            }

            return i;
        }
    }
}
