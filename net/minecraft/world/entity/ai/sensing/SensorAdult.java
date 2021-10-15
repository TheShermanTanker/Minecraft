package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorAdult extends Sensor<EntityAgeable> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    protected void doTick(WorldServer world, EntityAgeable entity) {
        entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((list) -> {
            this.setNearestVisibleAdult(entity, list);
        });
    }

    private void setNearestVisibleAdult(EntityAgeable entity, List<EntityLiving> visibleMobs) {
        Optional<EntityAgeable> optional = visibleMobs.stream().filter((livingEntity) -> {
            return livingEntity.getEntityType() == entity.getEntityType();
        }).map((livingEntity) -> {
            return (EntityAgeable)livingEntity;
        }).filter((ageableMob) -> {
            return !ageableMob.isBaby();
        }).findFirst();
        entity.getBehaviorController().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
    }
}
