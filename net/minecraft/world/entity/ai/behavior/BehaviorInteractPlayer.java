package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.player.EntityHuman;

public class BehaviorInteractPlayer extends Behavior<EntityVillager> {
    private final float speedModifier;

    public BehaviorInteractPlayer(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        EntityHuman player = entity.getTrader();
        return entity.isAlive() && player != null && !entity.isInWater() && !entity.hurtMarked && entity.distanceToSqr(player) <= 16.0D && player.containerMenu != null;
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.checkExtraStartConditions(serverLevel, villager);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityVillager villager, long l) {
        this.followPlayer(villager);
    }

    @Override
    protected void stop(WorldServer world, EntityVillager entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        brain.removeMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(WorldServer world, EntityVillager entity, long time) {
        this.followPlayer(entity);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    private void followPlayer(EntityVillager villager) {
        BehaviorController<?> brain = villager.getBehaviorController();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(villager.getTrader(), false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(villager.getTrader(), true));
    }
}
