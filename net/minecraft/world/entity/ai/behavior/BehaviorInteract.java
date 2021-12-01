package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

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
        return new BehaviorInteract<>(entityType, maxDistance, (entity) -> {
            return true;
        }, (entity) -> {
            return true;
        }, targetModule, speed, completionRange);
    }

    public static <T extends EntityLiving> BehaviorInteract<EntityLiving, T> of(EntityTypes<? extends T> entityType, int maxDistance, Predicate<T> condition, MemoryModuleType<T> moduleType, float speed, int completionRange) {
        return new BehaviorInteract<>(entityType, maxDistance, (entity) -> {
            return true;
        }, condition, moduleType, speed, completionRange);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.selfFilter.test(entity) && this.seesAtLeastOneValidTarget(entity);
    }

    private boolean seesAtLeastOneValidTarget(E entity) {
        NearestVisibleLivingEntities nearestVisibleLivingEntities = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
        return nearestVisibleLivingEntities.contains(this::isTargetValid);
    }

    private boolean isTargetValid(EntityLiving entity) {
        return this.type.equals(entity.getEntityType()) && this.targetFilter.test((T)entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Optional<NearestVisibleLivingEntities> optional = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (!optional.isEmpty()) {
            NearestVisibleLivingEntities nearestVisibleLivingEntities = optional.get();
            nearestVisibleLivingEntities.findClosest((target) -> {
                return this.canInteract(entity, target);
            }).ifPresent((target) -> {
                brain.setMemory(this.memory, (T)target);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
                brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(target, false), this.speedModifier, this.maxDist));
            });
        }
    }

    private boolean canInteract(E self, EntityLiving target) {
        return this.type.equals(target.getEntityType()) && target.distanceToSqr(self) <= (double)this.interactionRangeSqr && this.targetFilter.test((T)target);
    }
}
