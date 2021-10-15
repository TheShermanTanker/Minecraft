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
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorInteract<E extends EntityLiving, T extends EntityLiving> extends Behavior<E> {
    private final int maxDist;
    private final float speedModifier;
    private final EntityTypes<? extends T> type;
    private final int interactionRangeSqr;
    private final Predicate<T> targetFilter;
    private final Predicate<E> selfFilter;
    private final MemoryModuleType<T> memory;

    public BehaviorInteract(EntityTypes<? extends T> entityType, int maxDistance, Predicate<E> shouldRunPredicate, Predicate<T> predicate, MemoryModuleType<T> targetModule, float speed, int completionRange) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.type = entityType;
        this.speedModifier = speed;
        this.interactionRangeSqr = maxDistance * maxDistance;
        this.maxDist = completionRange;
        this.targetFilter = predicate;
        this.selfFilter = shouldRunPredicate;
        this.memory = targetModule;
    }

    public static <T extends EntityLiving> BehaviorInteract<EntityLiving, T> of(EntityTypes<? extends T> entityType, int maxDistance, MemoryModuleType<T> targetModule, float speed, int completionRange) {
        return new BehaviorInteract<>(entityType, maxDistance, (livingEntity) -> {
            return true;
        }, (livingEntity) -> {
            return true;
        }, targetModule, speed, completionRange);
    }

    public static <T extends EntityLiving> BehaviorInteract<EntityLiving, T> of(EntityTypes<? extends T> entityType, int maxDistance, Predicate<T> condition, MemoryModuleType<T> moduleType, float speed, int completionRange) {
        return new BehaviorInteract<>(entityType, maxDistance, (livingEntity) -> {
            return true;
        }, condition, moduleType, speed, completionRange);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.selfFilter.test(entity) && this.seesAtLeastOneValidTarget(entity);
    }

    private boolean seesAtLeastOneValidTarget(E entity) {
        List<EntityLiving> list = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
        return list.stream().anyMatch(this::isTargetValid);
    }

    private boolean isTargetValid(EntityLiving entity) {
        return this.type.equals(entity.getEntityType()) && this.targetFilter.test((T)entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((list) -> {
            list.stream().filter((livingEntity) -> {
                return this.type.equals(livingEntity.getEntityType());
            }).map((livingEntity) -> {
                return livingEntity;
            }).filter((livingEntity2) -> {
                return livingEntity2.distanceToSqr(entity) <= (double)this.interactionRangeSqr;
            }).filter(this.targetFilter).findFirst().ifPresent((livingEntity) -> {
                brain.setMemory(this.memory, (T)livingEntity);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(livingEntity, true));
                brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(livingEntity, false), this.speedModifier, this.maxDist));
            });
        });
    }
}
