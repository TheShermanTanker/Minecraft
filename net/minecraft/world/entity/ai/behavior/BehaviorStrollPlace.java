package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorStrollPlace extends Behavior<EntityCreature> {
    private final MemoryModuleType<GlobalPos> memoryType;
    private final int closeEnoughDist;
    private final int maxDistanceFromPoi;
    private final float speedModifier;
    private long nextOkStartTime;

    public BehaviorStrollPlace(MemoryModuleType<GlobalPos> memoryModuleType, float walkSpeed, int completionRange, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, memoryModuleType, MemoryStatus.VALUE_PRESENT));
        this.memoryType = memoryModuleType;
        this.speedModifier = walkSpeed;
        this.closeEnoughDist = completionRange;
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
            BehaviorController<?> brain = entity.getBehaviorController();
            Optional<GlobalPos> optional = brain.getMemory(this.memoryType);
            optional.ifPresent((globalPos) -> {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(globalPos.getBlockPosition(), this.speedModifier, this.closeEnoughDist));
            });
            this.nextOkStartTime = time + 80L;
        }

    }
}
