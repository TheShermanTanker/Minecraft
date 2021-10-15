package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class BehaviorStrollPosition extends Behavior<EntityCreature> {
    private static final int MIN_TIME_BETWEEN_STROLLS = 180;
    private static final int STROLL_MAX_XZ_DIST = 8;
    private static final int STROLL_MAX_Y_DIST = 6;
    private final MemoryModuleType<GlobalPos> memoryType;
    private long nextOkStartTime;
    private final int maxDistanceFromPoi;
    private final float speedModifier;

    public BehaviorStrollPosition(MemoryModuleType<GlobalPos> target, float walkSpeed, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, target, MemoryStatus.VALUE_PRESENT));
        this.memoryType = target;
        this.speedModifier = walkSpeed;
        this.maxDistanceFromPoi = maxDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        Optional<GlobalPos> optional = entity.getBehaviorController().getMemory(this.memoryType);
        return optional.isPresent() && world.getDimensionKey() == optional.get().getDimensionManager() && optional.get().getBlockPosition().closerThan(entity.getPositionVector(), (double)this.maxDistanceFromPoi);
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        if (time > this.nextOkStartTime) {
            Optional<Vec3D> optional = Optional.ofNullable(LandRandomPos.getPos(entity, 8, 6));
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
                return new MemoryTarget(vec3, this.speedModifier, 1);
            }));
            this.nextOkStartTime = time + 180L;
        }

    }
}
