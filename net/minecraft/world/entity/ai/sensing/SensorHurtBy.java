package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorHurtBy extends Sensor<EntityLiving> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        DamageSource damageSource = entity.getLastDamageSource();
        if (damageSource != null) {
            brain.setMemory(MemoryModuleType.HURT_BY, entity.getLastDamageSource());
            Entity entity2 = damageSource.getEntity();
            if (entity2 instanceof EntityLiving) {
                brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, (EntityLiving)entity2);
            }
        } else {
            brain.removeMemory(MemoryModuleType.HURT_BY);
        }

        brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent((livingEntity) -> {
            if (!livingEntity.isAlive() || livingEntity.level != world) {
                brain.removeMemory(MemoryModuleType.HURT_BY_ENTITY);
            }

        });
    }
}
