package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorPacify extends Behavior<EntityLiving> {
    private final int pacifyDuration;

    public BehaviorPacify(MemoryModuleType<?> requiredMemoryModuleType, int duration) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.PACIFIED, MemoryStatus.VALUE_ABSENT, requiredMemoryModuleType, MemoryStatus.VALUE_PRESENT));
        this.pacifyDuration = duration;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, (long)this.pacifyDuration);
        entity.getBehaviorController().removeMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
