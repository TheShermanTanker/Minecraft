package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.npc.EntityVillager;

public class BehaviorStrollPlaceList extends Behavior<EntityVillager> {
    private final MemoryModuleType<List<GlobalPos>> strollToMemoryType;
    private final MemoryModuleType<GlobalPos> mustBeCloseToMemoryType;
    private final float speedModifier;
    private final int closeEnoughDist;
    private final int maxDistanceFromPoi;
    private long nextOkStartTime;
    @Nullable
    private GlobalPos targetPos;

    public BehaviorStrollPlaceList(MemoryModuleType<List<GlobalPos>> secondaryPositions, float speed, int completionRange, int primaryPositionActivationDistance, MemoryModuleType<GlobalPos> primaryPosition) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, secondaryPositions, MemoryStatus.VALUE_PRESENT, primaryPosition, MemoryStatus.VALUE_PRESENT));
        this.strollToMemoryType = secondaryPositions;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
        this.maxDistanceFromPoi = primaryPositionActivationDistance;
        this.mustBeCloseToMemoryType = primaryPosition;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        Optional<List<GlobalPos>> optional = entity.getBehaviorController().getMemory(this.strollToMemoryType);
        Optional<GlobalPos> optional2 = entity.getBehaviorController().getMemory(this.mustBeCloseToMemoryType);
        if (optional.isPresent() && optional2.isPresent()) {
            List<GlobalPos> list = optional.get();
            if (!list.isEmpty()) {
                this.targetPos = list.get(world.getRandom().nextInt(list.size()));
                return this.targetPos != null && world.getDimensionKey() == this.targetPos.getDimensionManager() && optional2.get().getBlockPosition().closerThan(entity.getPositionVector(), (double)this.maxDistanceFromPoi);
            }
        }

        return false;
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        if (time > this.nextOkStartTime && this.targetPos != null) {
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(this.targetPos.getBlockPosition(), this.speedModifier, this.closeEnoughDist));
            this.nextOkStartTime = time + 100L;
        }

    }
}
