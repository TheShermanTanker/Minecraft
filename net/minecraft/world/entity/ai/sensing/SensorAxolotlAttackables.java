package net.minecraft.world.entity.ai.sensing;

import net.minecraft.tags.TagsEntity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorAxolotlAttackables extends SensorNearestVisibleEntityLiving {
    public static final float TARGET_DETECTION_DISTANCE = 8.0F;

    @Override
    protected boolean isMatchingEntity(EntityLiving entity, EntityLiving target) {
        return this.isClose(entity, target) && target.isInWaterOrBubble() && (this.isHostileTarget(target) || this.isHuntTarget(entity, target)) && Sensor.isEntityAttackable(entity, target);
    }

    private boolean isHuntTarget(EntityLiving axolotl, EntityLiving target) {
        return !axolotl.getBehaviorController().hasMemory(MemoryModuleType.HAS_HUNTING_COOLDOWN) && TagsEntity.AXOLOTL_HUNT_TARGETS.isTagged(target.getEntityType());
    }

    private boolean isHostileTarget(EntityLiving axolotl) {
        return TagsEntity.AXOLOTL_ALWAYS_HOSTILES.isTagged(axolotl.getEntityType());
    }

    private boolean isClose(EntityLiving axolotl, EntityLiving target) {
        return target.distanceToSqr(axolotl) <= 64.0D;
    }

    @Override
    protected MemoryModuleType<EntityLiving> getMemory() {
        return MemoryModuleType.NEAREST_ATTACKABLE;
    }
}
