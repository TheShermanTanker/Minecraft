package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentParticle;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.phys.Vec3D;

public class CommandParticle {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("particle").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("name", ArgumentParticle.particle()).executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), context.getSource().getPosition(), Vec3D.ZERO, 0.0F, 0, false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), Vec3D.ZERO, 0.0F, 0, false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(net.minecraft.commands.CommandDispatcher.argument("delta", ArgumentVec3.vec3(false)).then(net.minecraft.commands.CommandDispatcher.argument("speed", FloatArgumentType.floatArg(0.0F)).then(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(0)).executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), ArgumentVec3.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), ArgumentVec3.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), true, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(net.minecraft.commands.CommandDispatcher.argument("viewers", ArgumentEntity.players()).executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), ArgumentVec3.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), true, ArgumentEntity.getPlayers(context, "viewers"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), ArgumentVec3.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(net.minecraft.commands.CommandDispatcher.argument("viewers", ArgumentEntity.players()).executes((context) -> {
            return sendParticles(context.getSource(), ArgumentParticle.getParticle(context, "name"), ArgumentVec3.getVec3(context, "pos"), ArgumentVec3.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, ArgumentEntity.getPlayers(context, "viewers"));
        })))))))));
    }

    private static int sendParticles(CommandListenerWrapper source, ParticleParam parameters, Vec3D pos, Vec3D delta, float speed, int count, boolean force, Collection<EntityPlayer> viewers) throws CommandSyntaxException {
        int i = 0;

        for(EntityPlayer serverPlayer : viewers) {
            if (source.getWorld().sendParticles(serverPlayer, parameters, force, pos.x, pos.y, pos.z, count, delta.x, delta.y, delta.z, (double)speed)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendMessage(new ChatMessage("commands.particle.success", IRegistry.PARTICLE_TYPE.getKey(parameters.getParticle()).toString()), true);
            return i;
        }
    }
}
