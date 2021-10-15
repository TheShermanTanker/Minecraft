package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public abstract class SensorNearestVisibleEntityLiving extends Sensor<EntityLiving> {
    protected abstract boolean isMatchingEntity(EntityLiving entity, EntityLiving target);

    protected abstract MemoryModuleType<EntityLiving> getMemory();

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(this.getMemory());
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        entity.getBehaviorController().setMemory(this.getMemory(), this.getNearestEntity(entity));
    }

    private Optional<EntityLiving> getNearestEntity(EntityLiving entity) {
        return this.getVisibleEntities(entity).flatMap((list) -> {
            return list.stream().filter((livingEntity2) -> {
                return this.isMatchingEntity(entity, livingEntity2);
            }).min(Comparator.comparingDouble(entity::distanceToSqr));
        });
    }

    protected Optional<List<EntityLiving>> getVisibleEntities(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
