package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorAttackTargetSet<E extends EntityInsentient> extends Behavior<E> {
    private final Predicate<E> canAttackPredicate;
    private final Function<E, Optional<? extends EntityLiving>> targetFinderFunction;

    public BehaviorAttackTargetSet(Predicate<E> startCondition, Function<E, Optional<? extends EntityLiving>> targetGetter) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
        this.canAttackPredicate = startCondition;
        this.targetFinderFunction = targetGetter;
    }

    public BehaviorAttackTargetSet(Function<E, Optional<? extends EntityLiving>> targetGetter) {
        this((mob) -> {
            return true;
        }, targetGetter);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        if (!this.canAttackPredicate.test(entity)) {
            return false;
        } else {
            Optional<? extends EntityLiving> optional = this.targetFinderFunction.apply(entity);
            return optional.isPresent() ? entity.canAttack(optional.get()) : false;
        }
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        this.targetFinderFunction.apply(entity).ifPresent((livingEntity) -> {
            this.setAttackTarget(entity, livingEntity);
        });
    }

    private void setAttackTarget(E entity, EntityLiving target) {
        entity.getBehaviorController().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        entity.getBehaviorController().removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
