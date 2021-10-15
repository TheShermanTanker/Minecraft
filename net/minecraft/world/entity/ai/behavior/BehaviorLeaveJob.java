package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.pathfinder.PathEntity;

public class BehaviorLeaveJob extends Behavior<EntityVillager> {
    private final float speedModifier;

    public BehaviorLeaveJob(float speed) {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        if (entity.isBaby()) {
            return false;
        } else {
            return entity.getVillagerData().getProfession() == VillagerProfession.NONE;
        }
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        BlockPosition blockPos = entity.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().getBlockPosition();
        Optional<VillagePlaceType> optional = world.getPoiManager().getType(blockPos);
        if (optional.isPresent()) {
            BehaviorUtil.getNearbyVillagersWithCondition(entity, (villager) -> {
                return this.nearbyWantsJobsite(optional.get(), villager, blockPos);
            }).findFirst().ifPresent((villager2) -> {
                this.yieldJobSite(world, entity, villager2, blockPos, villager2.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE).isPresent());
            });
        }
    }

    private boolean nearbyWantsJobsite(VillagePlaceType poiType, EntityVillager villager, BlockPosition pos) {
        boolean bl = villager.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
        if (bl) {
            return false;
        } else {
            Optional<GlobalPos> optional = villager.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE);
            VillagerProfession villagerProfession = villager.getVillagerData().getProfession();
            if (villager.getVillagerData().getProfession() != VillagerProfession.NONE && villagerProfession.getJobPoiType().getPredicate().test(poiType)) {
                return !optional.isPresent() ? this.canReachPos(villager, pos, poiType) : optional.get().getBlockPosition().equals(pos);
            } else {
                return false;
            }
        }
    }

    private void yieldJobSite(WorldServer world, EntityVillager previousOwner, EntityVillager newOwner, BlockPosition pos, boolean jobSitePresent) {
        this.eraseMemories(previousOwner);
        if (!jobSitePresent) {
            BehaviorUtil.setWalkAndLookTargetMemories(newOwner, pos, this.speedModifier, 1);
            newOwner.getBehaviorController().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.create(world.getDimensionKey(), pos));
            PacketDebug.sendPoiTicketCountPacket(world, pos);
        }

    }

    private boolean canReachPos(EntityVillager villager, BlockPosition pos, VillagePlaceType poiType) {
        PathEntity path = villager.getNavigation().createPath(pos, poiType.getValidRange());
        return path != null && path.canReach();
    }

    private void eraseMemories(EntityVillager villager) {
        villager.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        villager.getBehaviorController().removeMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBehaviorController().removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }
}
