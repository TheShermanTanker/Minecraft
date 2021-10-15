package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntitySummon;
import net.minecraft.commands.arguments.ArgumentNBTTag;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class CommandSummon {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.summon.failed"));
    private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(new ChatMessage("commands.summon.failed.uuid"));
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new ChatMessage("commands.summon.invalidPosition"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("summon").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("entity", ArgumentEntitySummon.id()).suggests(CompletionProviders.SUMMONABLE_ENTITIES).executes((context) -> {
            return spawnEntity(context.getSource(), ArgumentEntitySummon.getSummonableEntity(context, "entity"), context.getSource().getPosition(), new NBTTagCompound(), true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).executes((context) -> {
            return spawnEntity(context.getSource(), ArgumentEntitySummon.getSummonableEntity(context, "entity"), ArgumentVec3.getVec3(context, "pos"), new NBTTagCompound(), true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("nbt", ArgumentNBTTag.compoundTag()).executes((context) -> {
            return spawnEntity(context.getSource(), ArgumentEntitySummon.getSummonableEntity(context, "entity"), ArgumentVec3.getVec3(context, "pos"), ArgumentNBTTag.getCompoundTag(context, "nbt"), false);
        })))));
    }

    private static int spawnEntity(CommandListenerWrapper source, MinecraftKey entity, Vec3D pos, NBTTagCompound nbt, boolean initialize) throws CommandSyntaxException {
        BlockPosition blockPos = new BlockPosition(pos);
        if (!World.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            NBTTagCompound compoundTag = nbt.c();
            compoundTag.setString("id", entity.toString());
            WorldServer serverLevel = source.getWorld();
            Entity entity2 = EntityTypes.loadEntityRecursive(compoundTag, serverLevel, (entityx) -> {
                entityx.setPositionRotation(pos.x, pos.y, pos.z, entityx.getYRot(), entityx.getXRot());
                return entityx;
            });
            if (entity2 == null) {
                throw ERROR_FAILED.create();
            } else {
                if (initialize && entity2 instanceof EntityInsentient) {
                    ((EntityInsentient)entity2).prepare(source.getWorld(), source.getWorld().getDamageScaler(entity2.getChunkCoordinates()), EnumMobSpawn.COMMAND, (GroupDataEntity)null, (NBTTagCompound)null);
                }

                if (!serverLevel.addAllEntitiesSafely(entity2)) {
                    throw ERROR_DUPLICATE_UUID.create();
                } else {
                    source.sendMessage(new ChatMessage("commands.summon.success", entity2.getScoreboardDisplayName()), true);
                    return 1;
                }
            }
        }
    }
}
