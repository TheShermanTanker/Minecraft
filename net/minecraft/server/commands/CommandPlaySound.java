package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutCustomSoundEffect;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.world.phys.Vec3D;

public class CommandPlaySound {
    private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(new ChatMessage("commands.playsound.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        RequiredArgumentBuilder<CommandListenerWrapper, MinecraftKey> requiredArgumentBuilder = net.minecraft.commands.CommandDispatcher.argument("sound", ArgumentMinecraftKeyRegistered.id()).suggests(CompletionProviders.AVAILABLE_SOUNDS);

        for(SoundCategory soundSource : SoundCategory.values()) {
            requiredArgumentBuilder.then(source(soundSource));
        }

        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("playsound").requires((source) -> {
            return source.hasPermission(2);
        }).then(requiredArgumentBuilder));
    }

    private static LiteralArgumentBuilder<CommandListenerWrapper> source(SoundCategory category) {
        return net.minecraft.commands.CommandDispatcher.literal(category.getName()).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.players()).executes((context) -> {
            return playSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentMinecraftKeyRegistered.getId(context, "sound"), category, context.getSource().getPosition(), 1.0F, 1.0F, 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).executes((context) -> {
            return playSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentMinecraftKeyRegistered.getId(context, "sound"), category, ArgumentVec3.getVec3(context, "pos"), 1.0F, 1.0F, 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("volume", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return playSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentMinecraftKeyRegistered.getId(context, "sound"), category, ArgumentVec3.getVec3(context, "pos"), context.getArgument("volume", Float.class), 1.0F, 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F)).executes((context) -> {
            return playSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentMinecraftKeyRegistered.getId(context, "sound"), category, ArgumentVec3.getVec3(context, "pos"), context.getArgument("volume", Float.class), context.getArgument("pitch", Float.class), 0.0F);
        }).then(net.minecraft.commands.CommandDispatcher.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F)).executes((context) -> {
            return playSound(context.getSource(), ArgumentEntity.getPlayers(context, "targets"), ArgumentMinecraftKeyRegistered.getId(context, "sound"), category, ArgumentVec3.getVec3(context, "pos"), context.getArgument("volume", Float.class), context.getArgument("pitch", Float.class), context.getArgument("minVolume", Float.class));
        }))))));
    }

    private static int playSound(CommandListenerWrapper source, Collection<EntityPlayer> targets, MinecraftKey sound, SoundCategory category, Vec3D pos, float volume, float pitch, float minVolume) throws CommandSyntaxException {
        double d = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
        int i = 0;
        Iterator var11 = targets.iterator();

        while(true) {
            EntityPlayer serverPlayer;
            Vec3D vec3;
            float j;
            while(true) {
                if (!var11.hasNext()) {
                    if (i == 0) {
                        throw ERROR_TOO_FAR.create();
                    }

                    if (targets.size() == 1) {
                        source.sendMessage(new ChatMessage("commands.playsound.success.single", sound, targets.iterator().next().getScoreboardDisplayName()), true);
                    } else {
                        source.sendMessage(new ChatMessage("commands.playsound.success.multiple", sound, targets.size()), true);
                    }

                    return i;
                }

                serverPlayer = (EntityPlayer)var11.next();
                double e = pos.x - serverPlayer.locX();
                double f = pos.y - serverPlayer.locY();
                double g = pos.z - serverPlayer.locZ();
                double h = e * e + f * f + g * g;
                vec3 = pos;
                j = volume;
                if (!(h > d)) {
                    break;
                }

                if (!(minVolume <= 0.0F)) {
                    double k = Math.sqrt(h);
                    vec3 = new Vec3D(serverPlayer.locX() + e / k * 2.0D, serverPlayer.locY() + f / k * 2.0D, serverPlayer.locZ() + g / k * 2.0D);
                    j = minVolume;
                    break;
                }
            }

            serverPlayer.connection.sendPacket(new PacketPlayOutCustomSoundEffect(sound, category, vec3, j, pitch));
            ++i;
        }
    }
}
