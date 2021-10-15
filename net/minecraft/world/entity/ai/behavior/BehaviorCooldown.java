package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.EntityVillager;

public class BehaviorCooldown extends Behavior<EntityVillager> {
    private static final int SAFE_DISTANCE_FROM_DANGER = 36;

    public BehaviorCooldown() {
        super(ImmutableMap.of());
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        boolean bl = BehaviorPanic.isHurt(entity) || BehaviorPanic.hasHostile(entity) || isCloseToEntityThatHurtMe(entity);
        if (!bl) {
            entity.getBehaviorController().removeMemory(MemoryModuleType.HURT_BY);
            entity.getBehaviorController().removeMemory(MemoryModuleType.HURT_BY_ENTITY);
            entity.getBehaviorController().updateActivityFromSchedule(world.getDayTime(), world.getTime());
        }

    }

    private static boolean isCloseToEntityThatHurtMe(EntityVillager entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.HURT_BY_ENTITY).filter((livingEntity) -> {
            return livingEntity.distanceToSqr(entity) <= 36.0D;
        }).isPresent();
    }
}
