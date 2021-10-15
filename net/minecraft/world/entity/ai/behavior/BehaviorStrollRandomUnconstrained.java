package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class BehaviorStrollRandomUnconstrained extends Behavior<EntityCreature> {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private final float speedModifier;
    protected final int maxHorizontalDistance;
    protected final int maxVerticalDistance;
    private final boolean mayStrollFromWater;

    public BehaviorStrollRandomUnconstrained(float speed) {
        this(speed, true);
    }

    public BehaviorStrollRandomUnconstrained(float speed, boolean strollInsideWater) {
        this(speed, 10, 7, strollInsideWater);
    }

    public BehaviorStrollRandomUnconstrained(float speed, int horizontalRadius, int verticalRadius) {
        this(speed, horizontalRadius, verticalRadius, true);
    }

    public BehaviorStrollRandomUnconstrained(float speed, int horizontalRadius, int verticalRadius, boolean strollInsideWater) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
        this.maxHorizontalDistance = horizontalRadius;
        this.maxVerticalDistance = verticalRadius;
        this.mayStrollFromWater = strollInsideWater;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return this.mayStrollFromWater || !entity.isInWaterOrBubble();
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        Optional<Vec3D> optional = Optional.ofNullable(this.getTargetPos(entity));
        entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, optional.map((pos) -> {
            return new MemoryTarget(pos, this.speedModifier, 0);
        }));
    }

    @Nullable
    protected Vec3D getTargetPos(EntityCreature entity) {
        return LandRandomPos.getPos(entity, this.maxHorizontalDistance, this.maxVerticalDistance);
    }
}
