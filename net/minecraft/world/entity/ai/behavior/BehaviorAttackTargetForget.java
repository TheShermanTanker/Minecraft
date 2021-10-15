package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorAttackTargetForget<E extends EntityInsentient> extends Behavior<E> {
    private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;
    private final Predicate<EntityLiving> stopAttackingWhen;
    private final Consumer<E> onTargetErased;

    public BehaviorAttackTargetForget(Predicate<EntityLiving> condition, Consumer<E> forgetCallback) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
        this.stopAttackingWhen = condition;
        this.onTargetErased = forgetCallback;
    }

    public BehaviorAttackTargetForget(Predicate<EntityLiving> alternativeCondition) {
        this(alternativeCondition, (mob) -> {
        });
    }

    public BehaviorAttackTargetForget(Consumer<E> forgetCallback) {
        this((livingEntity) -> {
            return false;
        }, forgetCallback);
    }

    public BehaviorAttackTargetForget() {
        this((livingEntity) -> {
            return false;
        }, (mob) -> {
        });
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        EntityLiving livingEntity = this.getAttackTarget(entity);
        if (!entity.canAttack(livingEntity)) {
            this.clearAttackTarget(entity);
        } else if (isTiredOfTryingToReachTarget(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.isCurrentTargetDeadOrRemoved(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.isCurrentTargetInDifferentLevel(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.stopAttackingWhen.test(this.getAttackTarget(entity))) {
            this.clearAttackTarget(entity);
        }
    }

    private boolean isCurrentTargetInDifferentLevel(E entity) {
        return this.getAttackTarget(entity).level != entity.level;
    }

    private EntityLiving getAttackTarget(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    private static <E extends EntityLiving> boolean isTiredOfTryingToReachTarget(E entity) {
        Optional<Long> optional = entity.getBehaviorController().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return optional.isPresent() && entity.level.getTime() - optional.get() > 200L;
    }

    private boolean isCurrentTargetDeadOrRemoved(E entity) {
        Optional<EntityLiving> optional = entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && !optional.get().isAlive();
    }

    protected void clearAttackTarget(E entity) {
        this.onTargetErased.accept(entity);
        entity.getBehaviorController().removeMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
