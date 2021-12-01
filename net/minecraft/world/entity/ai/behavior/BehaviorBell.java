package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorBell extends Behavior<EntityLiving> {
    private static final float SPEED_MODIFIER = 0.3F;

    public BehaviorBell() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Optional<GlobalPos> optional = brain.getMemory(MemoryModuleType.MEETING_POINT);
        return world.getRandom().nextInt(100) == 0 && optional.isPresent() && world.getDimensionKey() == optional.get().getDimensionManager() && optional.get().getBlockPosition().closerThan(entity.getPositionVector(), 4.0D) && brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains((livingEntity) -> {
            return EntityTypes.VILLAGER.equals(livingEntity.getEntityType());
        });
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((nearestVisibleLivingEntities) -> {
            return nearestVisibleLivingEntities.findClosest((livingEntity2) -> {
                return EntityTypes.VILLAGER.equals(livingEntity2.getEntityType()) && livingEntity2.distanceToSqr(entity) <= 32.0D;
            });
        }).ifPresent((livingEntity) -> {
            brain.setMemory(MemoryModuleType.INTERACTION_TARGET, livingEntity);
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(livingEntity, true));
            brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(livingEntity, false), 0.3F, 1));
        });
    }
}
