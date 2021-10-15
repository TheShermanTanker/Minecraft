package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorLookWalk extends Behavior<EntityLiving> {
    private final Function<EntityLiving, Float> speedModifier;
    private final int closeEnoughDistance;
    private final Predicate<EntityLiving> canSetWalkTargetPredicate;

    public BehaviorLookWalk(float speed, int completionRange) {
        this((entity) -> {
            return true;
        }, (entity) -> {
            return speed;
        }, completionRange);
    }

    public BehaviorLookWalk(Predicate<EntityLiving> predicate, Function<EntityLiving, Float> speed, int completionRange) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speed;
        this.closeEnoughDistance = completionRange;
        this.canSetWalkTargetPredicate = predicate;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return this.canSetWalkTargetPredicate.test(entity);
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        BehaviorPosition positionTracker = brain.getMemory(MemoryModuleType.LOOK_TARGET).get();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(positionTracker, this.speedModifier.apply(entity), this.closeEnoughDistance));
    }
}
