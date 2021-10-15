package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorVillagerHostiles extends SensorNearestVisibleEntityLiving {
    private static final ImmutableMap<EntityTypes<?>, Float> ACCEPTABLE_DISTANCE_FROM_HOSTILES = ImmutableMap.<EntityTypes<?>, Float>builder().put(EntityTypes.DROWNED, 8.0F).put(EntityTypes.EVOKER, 12.0F).put(EntityTypes.HUSK, 8.0F).put(EntityTypes.ILLUSIONER, 12.0F).put(EntityTypes.PILLAGER, 15.0F).put(EntityTypes.RAVAGER, 12.0F).put(EntityTypes.VEX, 8.0F).put(EntityTypes.VINDICATOR, 10.0F).put(EntityTypes.ZOGLIN, 10.0F).put(EntityTypes.ZOMBIE, 8.0F).put(EntityTypes.ZOMBIE_VILLAGER, 8.0F).build();

    @Override
    protected boolean isMatchingEntity(EntityLiving entity, EntityLiving target) {
        return this.isHostile(target) && this.isClose(entity, target);
    }

    private boolean isClose(EntityLiving villager, EntityLiving target) {
        float f = ACCEPTABLE_DISTANCE_FROM_HOSTILES.get(target.getEntityType());
        return target.distanceToSqr(villager) <= (double)(f * f);
    }

    @Override
    protected MemoryModuleType<EntityLiving> getMemory() {
        return MemoryModuleType.NEAREST_HOSTILE;
    }

    private boolean isHostile(EntityLiving entity) {
        return ACCEPTABLE_DISTANCE_FROM_HOSTILES.containsKey(entity.getEntityType());
    }
}
