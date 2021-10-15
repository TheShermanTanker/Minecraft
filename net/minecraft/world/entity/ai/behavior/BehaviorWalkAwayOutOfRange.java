package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorWalkAwayOutOfRange extends Behavior<EntityInsentient> {
    private static final int PROJECTILE_ATTACK_RANGE_BUFFER = 1;
    private final Function<EntityLiving, Float> speedModifier;

    public BehaviorWalkAwayOutOfRange(float speed) {
        this((livingEntity) -> {
            return speed;
        });
    }

    public BehaviorWalkAwayOutOfRange(Function<EntityLiving, Float> speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED));
        this.speedModifier = speed;
    }

    @Override
    protected void start(WorldServer world, EntityInsentient entity, long time) {
        EntityLiving livingEntity = entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        if (BehaviorUtil.canSee(entity, livingEntity) && BehaviorUtil.isWithinAttackRange(entity, livingEntity, 1)) {
            this.clearWalkTarget(entity);
        } else {
            this.setWalkAndLookTarget(entity, livingEntity);
        }

    }

    private void setWalkAndLookTarget(EntityLiving entity, EntityLiving target) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
        MemoryTarget walkTarget = new MemoryTarget(new BehaviorPositionEntity(target, false), this.speedModifier.apply(entity), 0);
        brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    private void clearWalkTarget(EntityLiving entity) {
        entity.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
    }
}
