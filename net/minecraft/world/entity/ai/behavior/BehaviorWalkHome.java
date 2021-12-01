package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.level.pathfinder.PathEntity;

public class BehaviorWalkHome extends Behavior<EntityLiving> {
    private static final int CACHE_TIMEOUT = 40;
    private static final int BATCH_SIZE = 5;
    private static final int RATE = 20;
    private static final int OK_DISTANCE_SQR = 4;
    private final float speedModifier;
    private final Long2LongMap batchCache = new Long2LongOpenHashMap();
    private int triedCount;
    private long lastUpdate;

    public BehaviorWalkHome(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        if (world.getTime() - this.lastUpdate < 20L) {
            return false;
        } else {
            EntityCreature pathfinderMob = (EntityCreature)entity;
            VillagePlace poiManager = world.getPoiManager();
            Optional<BlockPosition> optional = poiManager.findClosest(VillagePlaceType.HOME.getPredicate(), entity.getChunkCoordinates(), 48, VillagePlace.Occupancy.ANY);
            return optional.isPresent() && !(optional.get().distSqr(pathfinderMob.getChunkCoordinates()) <= 4.0D);
        }
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        this.triedCount = 0;
        this.lastUpdate = world.getTime() + (long)world.getRandom().nextInt(20);
        EntityCreature pathfinderMob = (EntityCreature)entity;
        VillagePlace poiManager = world.getPoiManager();
        Predicate<BlockPosition> predicate = (pos) -> {
            long l = pos.asLong();
            if (this.batchCache.containsKey(l)) {
                return false;
            } else if (++this.triedCount >= 5) {
                return false;
            } else {
                this.batchCache.put(l, this.lastUpdate + 40L);
                return true;
            }
        };
        Stream<BlockPosition> stream = poiManager.findAll(VillagePlaceType.HOME.getPredicate(), predicate, entity.getChunkCoordinates(), 48, VillagePlace.Occupancy.ANY);
        PathEntity path = pathfinderMob.getNavigation().createPath(stream, VillagePlaceType.HOME.getValidRange());
        if (path != null && path.canReach()) {
            BlockPosition blockPos = path.getTarget();
            Optional<VillagePlaceType> optional = poiManager.getType(blockPos);
            if (optional.isPresent()) {
                entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(blockPos, this.speedModifier, 1));
                PacketDebug.sendPoiTicketCountPacket(world, blockPos);
            }
        } else if (this.triedCount < 5) {
            this.batchCache.long2LongEntrySet().removeIf((entry) -> {
                return entry.getLongValue() < this.lastUpdate;
            });
        }

    }
}
