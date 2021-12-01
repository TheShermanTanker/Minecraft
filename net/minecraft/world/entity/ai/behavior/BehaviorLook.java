package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorLook extends Behavior<EntityInsentient> {
    public BehaviorLook(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        return mob.getBehaviorController().getMemory(MemoryModuleType.LOOK_TARGET).filter((lookTarget) -> {
            return lookTarget.isVisibleBy(mob);
        }).isPresent();
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityInsentient mob, long l) {
        mob.getBehaviorController().removeMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityInsentient mob, long l) {
        mob.getBehaviorController().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent((lookTarget) -> {
            mob.getControllerLook().setLookAt(lookTarget.currentPosition());
        });
    }
}
