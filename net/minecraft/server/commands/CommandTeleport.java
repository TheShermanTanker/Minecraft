package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.coordinates.ArgumentRotation;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.arguments.coordinates.IVectorPosition;
import net.minecraft.commands.arguments.coordinates.VectorPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class CommandTeleport {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new ChatMessage("commands.teleport.invalidPosition"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralCommandNode<CommandListenerWrapper> literalCommandNode = dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("teleport").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("location", ArgumentVec3.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), Collections.singleton(context.getSource().getEntityOrException()), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), VectorPosition.current(), (CommandTeleport.LookAt)null);
        })).then(net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentEntity.entity()).executes((context) -> {
            return teleportToEntity(context.getSource(), Collections.singleton(context.getSource().getEntityOrException()), ArgumentEntity.getEntity(context, "destination"));
        })).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("location", ArgumentVec3.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), ArgumentEntity.getEntities(context, "targets"), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), (IVectorPosition)null, (CommandTeleport.LookAt)null);
        }).then(net.minecraft.commands.CommandDispatcher.argument("rotation", ArgumentRotation.rotation()).executes((context) -> {
            return teleportToPos(context.getSource(), ArgumentEntity.getEntities(context, "targets"), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), ArgumentRotation.getRotation(context, "rotation"), (CommandTeleport.LookAt)null);
        })).then(net.minecraft.commands.CommandDispatcher.literal("facing").then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("facingEntity", ArgumentEntity.entity()).executes((context) -> {
            return teleportToPos(context.getSource(), ArgumentEntity.getEntities(context, "targets"), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), (IVectorPosition)null, new CommandTeleport.LookAt(ArgumentEntity.getEntity(context, "facingEntity"), ArgumentAnchor.Anchor.FEET));
        }).then(net.minecraft.commands.CommandDispatcher.argument("facingAnchor", ArgumentAnchor.anchor()).executes((context) -> {
            return teleportToPos(context.getSource(), ArgumentEntity.getEntities(context, "targets"), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), (IVectorPosition)null, new CommandTeleport.LookAt(ArgumentEntity.getEntity(context, "facingEntity"), ArgumentAnchor.getAnchor(context, "facingAnchor")));
        })))).then(net.minecraft.commands.CommandDispatcher.argument("facingLocation", ArgumentVec3.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), ArgumentEntity.getEntities(context, "targets"), context.getSource().getWorld(), ArgumentVec3.getCoordinates(context, "location"), (IVectorPosition)null, new CommandTeleport.LookAt(ArgumentVec3.getVec3(context, "facingLocation")));
        })))).then(net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentEntity.entity()).executes((context) -> {
            return teleportToEntity(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentEntity.getEntity(context, "destination"));
        }))));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("tp").requires((source) -> {
            return source.hasPermission(2);
        }).redirect(literalCommandNode));
    }

    private static int teleportToEntity(CommandListenerWrapper source, Collection<? extends Entity> targets, Entity destination) throws CommandSyntaxException {
        for(Entity entity : targets) {
            performTeleport(source, entity, (WorldServer)destination.level, destination.locX(), destination.locY(), destination.locZ(), EnumSet.noneOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class), destination.getYRot(), destination.getXRot(), (CommandTeleport.LookAt)null);
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.teleport.success.entity.single", targets.iterator().next().getScoreboardDisplayName(), destination.getScoreboardDisplayName()), true);
        } else {
            source.sendMessage(new ChatMessage("commands.teleport.success.entity.multiple", targets.size(), destination.getScoreboardDisplayName()), true);
        }

        return targets.size();
    }

    private static int teleportToPos(CommandListenerWrapper source, Collection<? extends Entity> targets, WorldServer world, IVectorPosition location, @Nullable IVectorPosition rotation, @Nullable CommandTeleport.LookAt facingLocation) throws CommandSyntaxException {
        Vec3D vec3 = location.getPosition(source);
        Vec2F vec2 = rotation == null ? null : rotation.getRotation(source);
        Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set = EnumSet.noneOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class);
        if (location.isXRelative()) {
            set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X);
        }

        if (location.isYRelative()) {
            set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y);
        }

        if (location.isZRelative()) {
            set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z);
        }

        if (rotation == null) {
            set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
            set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);
        } else {
            if (rotation.isXRelative()) {
                set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
            }

            if (rotation.isYRelative()) {
                set.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);
            }
        }

        for(Entity entity : targets) {
            if (rotation == null) {
                performTeleport(source, entity, world, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), facingLocation);
            } else {
                performTeleport(source, entity, world, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, facingLocation);
            }
        }

        if (targets.size() == 1) {
            source.sendMessage(new ChatMessage("commands.teleport.success.location.single", targets.iterator().next().getScoreboardDisplayName(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)), true);
        } else {
            source.sendMessage(new ChatMessage("commands.teleport.success.location.multiple", targets.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)), true);
        }

        return targets.size();
    }

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }

    private static void performTeleport(CommandListenerWrapper source, Entity target, WorldServer world, double x, double y, double z, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> movementFlags, float yaw, float pitch, @Nullable CommandTeleport.LookAt facingLocation) throws CommandSyntaxException {
        BlockPosition blockPos = new BlockPosition(x, y, z);
        if (!World.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = MathHelper.wrapDegrees(yaw);
            float g = MathHelper.wrapDegrees(pitch);
            if (target instanceof EntityPlayer) {
                ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(new BlockPosition(x, y, z));
                world.getChunkSource().addTicket(TicketType.POST_TELEPORT, chunkPos, 1, target.getId());
                target.stopRiding();
                if (((EntityPlayer)target).isSleeping()) {
                    ((EntityPlayer)target).wakeup(true, true);
                }

                if (world == target.level) {
                    ((EntityPlayer)target).connection.teleport(x, y, z, f, g, movementFlags);
                } else {
                    ((EntityPlayer)target).teleportTo(world, x, y, z, f, g);
                }

                target.setHeadRotation(f);
            } else {
                float h = MathHelper.clamp(g, -90.0F, 90.0F);
                if (world == target.level) {
                    target.setPositionRotation(x, y, z, f, h);
                    target.setHeadRotation(f);
                } else {
                    target.decouple();
                    Entity entity = target;
                    target = target.getEntityType().create(world);
                    if (target == null) {
                        return;
                    }

                    target.restoreFrom(entity);
                    target.setPositionRotation(x, y, z, f, h);
                    target.setHeadRotation(f);
                    entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                    world.addEntityTeleport(target);
                }
            }

            if (facingLocation != null) {
                facingLocation.perform(source, target);
            }

            if (!(target instanceof EntityLiving) || !((EntityLiving)target).isGliding()) {
                target.setMot(target.getMot().multiply(1.0D, 0.0D, 1.0D));
                target.setOnGround(true);
            }

            if (target instanceof EntityCreature) {
                ((EntityCreature)target).getNavigation().stop();
            }

        }
    }

    static class LookAt {
        private final Vec3D position;
        private final Entity entity;
        private final ArgumentAnchor.Anchor anchor;

        public LookAt(Entity target, ArgumentAnchor.Anchor targetAnchor) {
            this.entity = target;
            this.anchor = targetAnchor;
            this.position = targetAnchor.apply(target);
        }

        public LookAt(Vec3D targetPos) {
            this.entity = null;
            this.position = targetPos;
            this.anchor = null;
        }

        public void perform(CommandListenerWrapper source, Entity entity) {
            if (this.entity != null) {
                if (entity instanceof EntityPlayer) {
                    ((EntityPlayer)entity).lookAt(source.getAnchor(), this.entity, this.anchor);
                } else {
                    entity.lookAt(source.getAnchor(), this.position);
                }
            } else {
                entity.lookAt(source.getAnchor(), this.position);
            }

        }
    }
}
