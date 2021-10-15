package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;

public class BehaviorWork extends Behavior<EntityVillager> {
    private static final int CHECK_COOLDOWN = 300;
    private static final double DISTANCE = 1.73D;
    private long lastCheck;

    public BehaviorWork() {
        super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer serverLevel, EntityVillager villager) {
        if (serverLevel.getTime() - this.lastCheck < 300L) {
            return false;
        } else if (serverLevel.random.nextInt(2) != 0) {
            return false;
        } else {
            this.lastCheck = serverLevel.getTime();
            GlobalPos globalPos = villager.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE).get();
            return globalPos.getDimensionManager() == serverLevel.getDimensionKey() && globalPos.getBlockPosition().closerThan(villager.getPositionVector(), 1.73D);
        }
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        BehaviorController<EntityVillager> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, time);
        brain.getMemory(MemoryModuleType.JOB_SITE).ifPresent((globalPos) -> {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(globalPos.getBlockPosition()));
        });
        entity.playWorkSound();
        this.doWork(world, entity);
        if (entity.shouldRestock()) {
            entity.restock();
        }

    }

    protected void doWork(WorldServer world, EntityVillager entity) {
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityVillager entity, long time) {
        Optional<GlobalPos> optional = entity.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE);
        if (!optional.isPresent()) {
            return false;
        } else {
            GlobalPos globalPos = optional.get();
            return globalPos.getDimensionManager() == world.getDimensionKey() && globalPos.getBlockPosition().closerThan(entity.getPositionVector(), 1.73D);
        }
    }
}
