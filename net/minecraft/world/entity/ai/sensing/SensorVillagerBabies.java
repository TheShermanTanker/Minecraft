package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorVillagerBabies extends Sensor<EntityLiving> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        entity.getBehaviorController().setMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES, this.getNearestVillagerBabies(entity));
    }

    private List<EntityLiving> getNearestVillagerBabies(EntityLiving entities) {
        return this.getVisibleEntities(entities).stream().filter(this::isVillagerBaby).collect(Collectors.toList());
    }

    private boolean isVillagerBaby(EntityLiving entity) {
        return entity.getEntityType() == EntityTypes.VILLAGER && entity.isBaby();
    }

    private List<EntityLiving> getVisibleEntities(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(Lists.newArrayList());
    }
}
