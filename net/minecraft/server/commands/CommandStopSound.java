package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutStopSound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;

public class CommandStopSound {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        RequiredArgumentBuilder<CommandListenerWrapper, EntitySelector> requiredArgumentBuilder = net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return stopSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), (SoundCategory)null, (MinecraftKey)null);
        }).then(net.minecraft.commands.CommandDispatcher.literal("*").then(net.minecraft.commands.CommandDispatcher.argument("sound", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.AVAILABLE_SOUNDS).executes((context) -> {
            return stopSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), (SoundCategory)null, ArgumentMinecraftKeyRegistered.getId(context, "sound"));
        })));

        for(SoundCategory soundSource : SoundCategory.values()) {
            requiredArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(soundSource.getName()).executes((context) -> {
                return stopSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), soundSource, (MinecraftKey)null);
            }).then(net.minecraft.commands.CommandDispatcher.argument("sound", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.AVAILABLE_SOUNDS).executes((context) -> {
                return stopSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), soundSource, ArgumentMinecraftKeyRegistered.getId(context, "sound"));
            })));
        }

        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("stopsound").requires((source) -> {
            return source.hasPermission(2);
        }).then(requiredArgumentBuilder));
    }

    private static int stopSound(CommandListenerWrapper source, Collection<EntityPlayer> targets, @Nullable SoundCategory category, @Nullable MinecraftKey sound) {
        PacketPlayOutStopSound clientboundStopSoundPacket = new PacketPlayOutStopSound(sound, category);

        for(EntityPlayer serverPlayer : targets) {
            serverPlayer.connection.sendPacket(clientboundStopSoundPacket);
        }

        if (category != null) {
            if (sound != null) {
                source.sendMessage(new ChatMessage("commands.stopsound.success.source.sound", sound, category.getName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.stopsound.success.source.any", category.getName()), true);
            }
        } else if (sound != null) {
            source.sendMessage(new ChatMessage("commands.stopsound.success.sourceless.sound", sound), true);
        } else {
            source.sendMessage(new ChatMessage("commands.stopsound.success.sourceless.any"), true);
        }

        return targets.size();
    }
}
