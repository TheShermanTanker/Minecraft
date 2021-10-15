package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AxisAlignedBB;

public class SensorNearestLivingEntities extends Sensor<EntityLiving> {
    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        AxisAlignedBB aABB = entity.getBoundingBox().grow(16.0D, 16.0D, 16.0D);
        List<EntityLiving> list = world.getEntitiesOfClass(EntityLiving.class, aABB, (livingEntity2) -> {
            return livingEntity2 != entity && livingEntity2.isAlive();
        });
        list.sort(Comparator.comparingDouble(entity::distanceToSqr));
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, list);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, list.stream().filter((livingEntity2) -> {
            return isEntityTargetable(entity, livingEntity2);
        }).collect(Collectors.toList()));
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
