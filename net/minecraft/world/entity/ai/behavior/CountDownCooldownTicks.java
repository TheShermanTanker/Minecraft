package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CountDownCooldownTicks extends Behavior<EntityLiving> {
    private final MemoryModuleType<Integer> cooldownTicks;

    public CountDownCooldownTicks(MemoryModuleType<Integer> moduleType) {
        super(ImmutableMap.of(moduleType, MemoryStatus.VALUE_PRESENT));
        this.cooldownTicks = moduleType;
    }

    private Optional<Integer> getCooldownTickMemory(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(this.cooldownTicks);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityLiving entity, long time) {
        Optional<Integer> optional = this.getCooldownTickMemory(entity);
        return optional.isPresent() && optional.get() > 0;
    }

    @Override
    protected void tick(WorldServer world, EntityLiving entity, long time) {
        Optional<Integer> optional = this.getCooldownTickMemory(entity);
        entity.getBehaviorController().setMemory(this.cooldownTicks, optional.get() - 1);
    }

    @Override
    protected void stop(WorldServer world, EntityLiving entity, long time) {
        entity.getBehaviorController().removeMemory(this.cooldownTicks);
    }
}
