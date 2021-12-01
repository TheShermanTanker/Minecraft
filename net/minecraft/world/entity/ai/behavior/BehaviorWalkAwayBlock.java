package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.phys.Vec3D;

public class BehaviorWalkAwayBlock extends Behavior<EntityVillager> {
    private final MemoryModuleType<GlobalPos> memoryType;
    private final float speedModifier;
    private final int closeEnoughDist;
    private final int tooFarDistance;
    private final int tooLongUnreachableDuration;

    public BehaviorWalkAwayBlock(MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxRange, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, destination, MemoryStatus.VALUE_PRESENT));
        this.memoryType = destination;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
        this.tooFarDistance = maxRange;
        this.tooLongUnreachableDuration = maxRunTime;
    }

    private void dropPOI(EntityVillager villager, long time) {
        BehaviorController<?> brain = villager.getBehaviorController();
        villager.releasePoi(this.memoryType);
        brain.removeMemory(this.memoryType);
        brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(this.memoryType).ifPresent((pos) -> {
            if (!this.wrongDimension(world, pos) && !this.tiredOfTryingToFindTarget(world, entity)) {
                if (this.tooFar(entity, pos)) {
                    Vec3D vec3 = null;
                    int i = 0;

                    for(int j = 1000; i < 1000 && (vec3 == null || this.tooFar(entity, GlobalPos.create(world.getDimensionKey(), new BlockPosition(vec3)))); ++i) {
                        vec3 = DefaultRandomPos.getPosTowards(entity, 15, 7, Vec3D.atBottomCenterOf(pos.getBlockPosition()), (double)((float)Math.PI / 2F));
                    }

                    if (i == 1000) {
                        this.dropPOI(entity, time);
                        return;
                    }

                    brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, this.speedModifier, this.closeEnoughDist));
                } else if (!this.closeEnough(world, entity, pos)) {
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(pos.getBlockPosition(), this.speedModifier, this.closeEnoughDist));
                }
            } else {
                this.dropPOI(entity, time);
            }

        });
    }

    private boolean tiredOfTryingToFindTarget(WorldServer world, EntityVillager villager) {
        Optional<Long> optional = villager.getBehaviorController().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        if (optional.isPresent()) {
            return world.getTime() - optional.get() > (long)this.tooLongUnreachableDuration;
        } else {
            return false;
        }
    }

    private boolean tooFar(EntityVillager villager, GlobalPos pos) {
        return pos.getBlockPosition().distManhattan(villager.getChunkCoordinates()) > this.tooFarDistance;
    }

    private boolean wrongDimension(WorldServer world, GlobalPos pos) {
        return pos.getDimensionManager() != world.getDimensionKey();
    }

    private boolean closeEnough(WorldServer world, EntityVillager villager, GlobalPos pos) {
        return pos.getDimensionManager() == world.getDimensionKey() && pos.getBlockPosition().distManhattan(villager.getChunkCoordinates()) <= this.closeEnoughDist;
    }
}
