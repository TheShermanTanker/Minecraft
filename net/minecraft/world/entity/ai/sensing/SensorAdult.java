package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SensorAdult extends Sensor<EntityAgeable> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    protected void doTick(WorldServer world, EntityAgeable entity) {
        entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((nearestVisibleLivingEntities) -> {
            this.setNearestVisibleAdult(entity, nearestVisibleLivingEntities);
        });
    }

    private void setNearestVisibleAdult(EntityAgeable entity, NearestVisibleLivingEntities nearestVisibleLivingEntities) {
        Optional<EntityAgeable> optional = nearestVisibleLivingEntities.findClosest((livingEntity) -> {
            return livingEntity.getEntityType() == entity.getEntityType() && !livingEntity.isBaby();
        }).map(EntityAgeable.class::cast);
        entity.getBehaviorController().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
    }
}
