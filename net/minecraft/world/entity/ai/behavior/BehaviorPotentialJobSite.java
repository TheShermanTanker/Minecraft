package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.schedule.Activity;

public class BehaviorPotentialJobSite extends Behavior<EntityVillager> {
    private static final int TICKS_UNTIL_TIMEOUT = 1200;
    final float speedModifier;

    public BehaviorPotentialJobSite(float speed) {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT), 1200);
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        return entity.getBehaviorController().getActiveNonCoreActivity().map((activity) -> {
            return activity == Activity.IDLE || activity == Activity.WORK || activity == Activity.PLAY;
        }).orElse(true);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return villager.getBehaviorController().hasMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        BehaviorUtil.setWalkAndLookTargetMemories(villager, villager.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().getBlockPosition(), this.speedModifier, 1);
    }

    @Override
    protected void stop(WorldServer world, EntityVillager entity, long time) {
        Optional<GlobalPos> optional = entity.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        optional.ifPresent((globalPos) -> {
            BlockPosition blockPos = globalPos.getBlockPosition();
            WorldServer serverLevel2 = world.getMinecraftServer().getWorldServer(globalPos.getDimensionManager());
            if (serverLevel2 != null) {
                VillagePlace poiManager = serverLevel2.getPoiManager();
                if (poiManager.exists(blockPos, (poiType) -> {
                    return true;
                })) {
                    poiManager.release(blockPos);
                }

                PacketDebug.sendPoiTicketCountPacket(world, blockPos);
            }
        });
        entity.getBehaviorController().removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }
}
