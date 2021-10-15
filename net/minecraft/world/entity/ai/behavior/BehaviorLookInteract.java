package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorLookInteract extends Behavior<EntityLiving> {
    private final EntityTypes<?> type;
    private final int interactionRangeSqr;
    private final Predicate<EntityLiving> targetFilter;
    private final Predicate<EntityLiving> selfFilter;

    public BehaviorLookInteract(EntityTypes<?> entityType, int maxDistance, Predicate<EntityLiving> shouldRunPredicate, Predicate<EntityLiving> predicate) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.type = entityType;
        this.interactionRangeSqr = maxDistance * maxDistance;
        this.targetFilter = predicate;
        this.selfFilter = shouldRunPredicate;
    }

    public BehaviorLookInteract(EntityTypes<?> entityType, int maxDistance) {
        this(entityType, maxDistance, (livingEntity) -> {
            return true;
        }, (livingEntity) -> {
            return true;
        });
    }

    @Override
    public boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return this.selfFilter.test(entity) && this.getVisibleEntities(entity).stream().anyMatch(this::isMatchingTarget);
    }

    @Override
    public void start(WorldServer world, EntityLiving entity, long time) {
        super.start(world, entity, time);
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((list) -> {
            list.stream().filter((livingEntity2) -> {
                return livingEntity2.distanceToSqr(entity) <= (double)this.interactionRangeSqr;
            }).filter(this::isMatchingTarget).findFirst().ifPresent((livingEntity) -> {
                brain.setMemory(MemoryModuleType.INTERACTION_TARGET, livingEntity);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(livingEntity, true));
            });
        });
    }

    private boolean isMatchingTarget(EntityLiving entity) {
        return this.type.equals(entity.getEntityType()) && this.targetFilter.test(entity);
    }

    private List<EntityLiving> getVisibleEntities(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
    }
}
