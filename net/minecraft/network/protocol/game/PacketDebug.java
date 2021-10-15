package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.UtilColor;
import net.minecraft.world.IInventory;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorPositionEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.gossip.ReputationType;
import net.minecraft.world.entity.ai.memory.ExpirableMemory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.PathEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketDebug {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void sendGameTestAddMarker(WorldServer world, BlockPosition pos, String message, int color, int duration) {
        PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(Unpooled.buffer());
        friendlyByteBuf.writeBlockPos(pos);
        friendlyByteBuf.writeInt(color);
        friendlyByteBuf.writeUtf(message);
        friendlyByteBuf.writeInt(duration);
        sendPacketToAllPlayers(world, friendlyByteBuf, PacketPlayOutCustomPayload.DEBUG_GAME_TEST_ADD_MARKER);
    }

    public static void sendGameTestClearPacket(WorldServer world) {
        PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(Unpooled.buffer());
        sendPacketToAllPlayers(world, friendlyByteBuf, PacketPlayOutCustomPayload.DEBUG_GAME_TEST_CLEAR);
    }

    public static void sendPoiPacketsForChunk(WorldServer world, ChunkCoordIntPair pos) {
    }

    public static void sendPoiAddedPacket(WorldServer world, BlockPosition pos) {
        sendVillageSectionsPacket(world, pos);
    }

    public static void sendPoiRemovedPacket(WorldServer world, BlockPosition pos) {
        sendVillageSectionsPacket(world, pos);
    }

    public static void sendPoiTicketCountPacket(WorldServer world, BlockPosition pos) {
        sendVillageSectionsPacket(world, pos);
    }

    private static void sendVillageSectionsPacket(WorldServer world, BlockPosition pos) {
    }

    public static void sendPathFindingPacket(World world, EntityInsentient mob, @Nullable PathEntity path, float nodeReachProximity) {
    }

    public static void sendNeighborsUpdatePacket(World world, BlockPosition pos) {
    }

    public static void sendStructurePacket(GeneratorAccessSeed world, StructureStart<?> structureStart) {
    }

    public static void sendGoalSelector(World world, EntityInsentient mob, PathfinderGoalSelector goalSelector) {
        if (world instanceof WorldServer) {
            ;
        }
    }

    public static void sendRaids(WorldServer server, Collection<Raid> raids) {
    }

    public static void sendEntityBrain(EntityLiving living) {
    }

    public static void sendBeeInfo(EntityBee bee) {
    }

    public static void sendGameEventInfo(World world, GameEvent event, BlockPosition pos) {
    }

    public static void sendGameEventListenerInfo(World world, GameEventListener eventListener) {
    }

    public static void sendHiveInfo(World world, BlockPosition pos, IBlockData state, TileEntityBeehive blockEntity) {
    }

    private static void writeBrain(EntityLiving entity, PacketDataSerializer buf) {
        BehaviorController<?> brain = entity.getBehaviorController();
        long l = entity.level.getTime();
        if (entity instanceof InventoryCarrier) {
            IInventory container = ((InventoryCarrier)entity).getInventory();
            buf.writeUtf(container.isEmpty() ? "" : container.toString());
        } else {
            buf.writeUtf("");
        }

        if (brain.hasMemory(MemoryModuleType.PATH)) {
            buf.writeBoolean(true);
            PathEntity path = brain.getMemory(MemoryModuleType.PATH).get();
            path.writeToStream(buf);
        } else {
            buf.writeBoolean(false);
        }

        if (entity instanceof EntityVillager) {
            EntityVillager villager = (EntityVillager)entity;
            boolean bl = villager.wantsToSpawnGolem(l);
            buf.writeBoolean(bl);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeCollection(brain.getActiveActivities(), (bufx, activity) -> {
            bufx.writeUtf(activity.getName());
        });
        Set<String> set = brain.getRunningBehaviors().stream().map(Behavior::toString).collect(Collectors.toSet());
        buf.writeCollection(set, PacketDataSerializer::writeUtf);
        buf.writeCollection(getMemoryDescriptions(entity, l), (bufx, memory) -> {
            String string = UtilColor.truncateStringIfNecessary(memory, 255, true);
            bufx.writeUtf(string);
        });
        if (entity instanceof EntityVillager) {
            Set<BlockPosition> set2 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT).map(brain::getMemory).flatMap(SystemUtils::toStream).map(GlobalPos::getBlockPosition).collect(Collectors.toSet());
            buf.writeCollection(set2, PacketDataSerializer::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }

        if (entity instanceof EntityVillager) {
            Set<BlockPosition> set3 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE).map(brain::getMemory).flatMap(SystemUtils::toStream).map(GlobalPos::getBlockPosition).collect(Collectors.toSet());
            buf.writeCollection(set3, PacketDataSerializer::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }

        if (entity instanceof EntityVillager) {
            Map<UUID, Object2IntMap<ReputationType>> map = ((EntityVillager)entity).getGossips().getGossipEntries();
            List<String> list = Lists.newArrayList();
            map.forEach((uuid, gossips) -> {
                String string = DebugEntityNameGenerator.getEntityName(uuid);
                gossips.forEach((type, value) -> {
                    list.add(string + ": " + type + ": " + value);
                });
            });
            buf.writeCollection(list, PacketDataSerializer::writeUtf);
        } else {
            buf.writeVarInt(0);
        }

    }

    private static List<String> getMemoryDescriptions(EntityLiving entity, long currentTime) {
        Map<MemoryModuleType<?>, Optional<? extends ExpirableMemory<?>>> map = entity.getBehaviorController().getMemories();
        List<String> list = Lists.newArrayList();

        for(Entry<MemoryModuleType<?>, Optional<? extends ExpirableMemory<?>>> entry : map.entrySet()) {
            MemoryModuleType<?> memoryModuleType = entry.getKey();
            Optional<? extends ExpirableMemory<?>> optional = entry.getValue();
            String string;
            if (optional.isPresent()) {
                ExpirableMemory<?> expirableValue = optional.get();
                Object object = expirableValue.getValue();
                if (memoryModuleType == MemoryModuleType.HEARD_BELL_TIME) {
                    long l = currentTime - (Long)object;
                    string = l + " ticks ago";
                } else if (expirableValue.canExpire()) {
                    string = getShortDescription((WorldServer)entity.level, object) + " (ttl: " + expirableValue.getTimeToLive() + ")";
                } else {
                    string = getShortDescription((WorldServer)entity.level, object);
                }
            } else {
                string = "-";
            }

            list.add(IRegistry.MEMORY_MODULE_TYPE.getKey(memoryModuleType).getKey() + ": " + string);
        }

        list.sort(String::compareTo);
        return list;
    }

    private static String getShortDescription(WorldServer world, @Nullable Object object) {
        if (object == null) {
            return "-";
        } else if (object instanceof UUID) {
            return getShortDescription(world, world.getEntity((UUID)object));
        } else if (object instanceof EntityLiving) {
            Entity entity = (Entity)object;
            return DebugEntityNameGenerator.getEntityName(entity);
        } else if (object instanceof INamableTileEntity) {
            return ((INamableTileEntity)object).getDisplayName().getString();
        } else if (object instanceof MemoryTarget) {
            return getShortDescription(world, ((MemoryTarget)object).getTarget());
        } else if (object instanceof BehaviorPositionEntity) {
            return getShortDescription(world, ((BehaviorPositionEntity)object).getEntity());
        } else if (object instanceof GlobalPos) {
            return getShortDescription(world, ((GlobalPos)object).getBlockPosition());
        } else if (object instanceof BehaviorTarget) {
            return getShortDescription(world, ((BehaviorTarget)object).currentBlockPosition());
        } else if (object instanceof EntityDamageSource) {
            Entity entity2 = ((EntityDamageSource)object).getEntity();
            return entity2 == null ? object.toString() : getShortDescription(world, entity2);
        } else if (!(object instanceof Collection)) {
            return object.toString();
        } else {
            List<String> list = Lists.newArrayList();

            for(Object object2 : (Iterable)object) {
                list.add(getShortDescription(world, object2));
            }

            return list.toString();
        }
    }

    private static void sendPacketToAllPlayers(WorldServer world, PacketDataSerializer buf, MinecraftKey channel) {
        Packet<?> packet = new PacketPlayOutCustomPayload(channel, buf);

        for(EntityHuman player : world.getLevel().getPlayers()) {
            ((EntityPlayer)player).connection.sendPacket(packet);
        }

    }
}
