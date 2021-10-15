package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class BehaviorWalkAway<T> extends Behavior<EntityCreature> {
    private final MemoryModuleType<T> walkAwayFromMemory;
    private final float speedModifier;
    private final int desiredDistance;
    private final Function<T, Vec3D> toPosition;

    public BehaviorWalkAway(MemoryModuleType<T> memoryType, float speed, int range, boolean requiresWalkTarget, Function<T, Vec3D> posRetriever) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, requiresWalkTarget ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, memoryType, MemoryStatus.VALUE_PRESENT));
        this.walkAwayFromMemory = memoryType;
        this.speedModifier = speed;
        this.desiredDistance = range;
        this.toPosition = posRetriever;
    }

    public static BehaviorWalkAway<BlockPosition> pos(MemoryModuleType<BlockPosition> memoryType, float speed, int range, boolean requiresWalkTarget) {
        return new BehaviorWalkAway<>(memoryType, speed, range, requiresWalkTarget, Vec3D::atBottomCenterOf);
    }

    public static BehaviorWalkAway<? extends Entity> entity(MemoryModuleType<? extends Entity> memoryType, float speed, int range, boolean requiresWalkTarget) {
        return new BehaviorWalkAway<>(memoryType, speed, range, requiresWalkTarget, Entity::getPositionVector);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return this.alreadyWalkingAwayFromPosWithSameSpeed(entity) ? false : entity.getPositionVector().closerThan(this.getPosToAvoid(entity), (double)this.desiredDistance);
    }

    private Vec3D getPosToAvoid(EntityCreature entity) {
        return this.toPosition.apply(entity.getBehaviorController().getMemory(this.walkAwayFromMemory).get());
    }

    private boolean alreadyWalkingAwayFromPosWithSameSpeed(EntityCreature entity) {
        if (!entity.getBehaviorController().hasMemory(MemoryModuleType.WALK_TARGET)) {
            return false;
        } else {
            MemoryTarget walkTarget = entity.getBehaviorController().getMemory(MemoryModuleType.WALK_TARGET).get();
            if (walkTarget.getSpeedModifier() != this.speedModifier) {
                return false;
            } else {
                Vec3D vec3 = walkTarget.getTarget().currentPosition().subtract(entity.getPositionVector());
                Vec3D vec32 = this.getPosToAvoid(entity).subtract(entity.getPositionVector());
                return vec3.dot(vec32) < 0.0D;
            }
        }
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        moveAwayFrom(entity, this.getPosToAvoid(entity), this.speedModifier);
    }

    private static void moveAwayFrom(EntityCreature entity, Vec3D pos, float speed) {
        for(int i = 0; i < 10; ++i) {
            Vec3D vec3 = LandRandomPos.getPosAway(entity, 16, 7, pos);
            if (vec3 != null) {
                entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, speed, 0));
                return;
            }
        }

    }
}
