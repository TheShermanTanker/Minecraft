package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.EntityItem;

public class BehaviorFindAdmirableItem<E extends EntityLiving> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final int maxDistToWalk;
    private final float speedModifier;

    public BehaviorFindAdmirableItem(float speed, boolean requiresWalkTarget, int radius) {
        this((livingEntity) -> {
            return true;
        }, speed, requiresWalkTarget, radius);
    }

    public BehaviorFindAdmirableItem(Predicate<E> startCondition, float speed, boolean requiresWalkTarget, int radius) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, requiresWalkTarget ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT));
        this.predicate = startCondition;
        this.maxDistToWalk = radius;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.predicate.test(entity) && this.getClosestLovedItem(entity).closerThan(entity, (double)this.maxDistToWalk);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorUtil.setWalkAndLookTargetMemories(entity, this.getClosestLovedItem(entity), this.speedModifier, 0);
    }

    private EntityItem getClosestLovedItem(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
    }
}
