package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorRememberHuntedHoglin<E extends EntityPiglin> extends Behavior<E> {
    public BehaviorRememberHuntedHoglin() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.HUNTED_RECENTLY, MemoryStatus.REGISTERED));
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        if (this.isAttackTargetDeadHoglin(entity)) {
            PiglinAI.dontKillAnyMoreHoglinsForAWhile(entity);
        }

    }

    private boolean isAttackTargetDeadHoglin(E piglin) {
        EntityLiving livingEntity = piglin.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        return livingEntity.getEntityType() == EntityTypes.HOGLIN && livingEntity.isDeadOrDying();
    }
}
