package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorBedJump extends Behavior<EntityInsentient> {
    private static final int MAX_TIME_TO_REACH_BED = 100;
    private static final int MIN_JUMPS = 3;
    private static final int MAX_JUMPS = 6;
    private static final int COOLDOWN_BETWEEN_JUMPS = 5;
    private final float speedModifier;
    @Nullable
    private BlockPosition targetBed;
    private int remainingTimeToReachBed;
    private int remainingJumps;
    private int remainingCooldownUntilNextJump;

    public BehaviorBedJump(float walkSpeed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_BED, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = walkSpeed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityInsentient entity) {
        return entity.isBaby() && this.nearBed(world, entity);
    }

    @Override
    protected void start(WorldServer world, EntityInsentient entity, long time) {
        super.start(world, entity, time);
        this.getNearestBed(entity).ifPresent((pos) -> {
            this.targetBed = pos;
            this.remainingTimeToReachBed = 100;
            this.remainingJumps = 3 + world.random.nextInt(4);
            this.remainingCooldownUntilNextJump = 0;
            this.startWalkingTowardsBed(entity, pos);
        });
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityInsentient mob, long l) {
        super.stop(serverLevel, mob, l);
        this.targetBed = null;
        this.remainingTimeToReachBed = 0;
        this.remainingJumps = 0;
        this.remainingCooldownUntilNextJump = 0;
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        return mob.isBaby() && this.targetBed != null && this.isBed(serverLevel, this.targetBed) && !this.tiredOfWalking(serverLevel, mob) && !this.tiredOfJumping(serverLevel, mob);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void tick(WorldServer world, EntityInsentient entity, long time) {
        if (!this.onOrOverBed(world, entity)) {
            --this.remainingTimeToReachBed;
        } else if (this.remainingCooldownUntilNextJump > 0) {
            --this.remainingCooldownUntilNextJump;
        } else {
            if (this.onBedSurface(world, entity)) {
                entity.getControllerJump().jump();
                --this.remainingJumps;
                this.remainingCooldownUntilNextJump = 5;
            }

        }
    }

    private void startWalkingTowardsBed(EntityInsentient mob, BlockPosition pos) {
        mob.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(pos, this.speedModifier, 0));
    }

    private boolean nearBed(WorldServer world, EntityInsentient mob) {
        return this.onOrOverBed(world, mob) || this.getNearestBed(mob).isPresent();
    }

    private boolean onOrOverBed(WorldServer world, EntityInsentient mob) {
        BlockPosition blockPos = mob.getChunkCoordinates();
        BlockPosition blockPos2 = blockPos.below();
        return this.isBed(world, blockPos) || this.isBed(world, blockPos2);
    }

    private boolean onBedSurface(WorldServer world, EntityInsentient mob) {
        return this.isBed(world, mob.getChunkCoordinates());
    }

    private boolean isBed(WorldServer world, BlockPosition pos) {
        return world.getType(pos).is(TagsBlock.BEDS);
    }

    private Optional<BlockPosition> getNearestBed(EntityInsentient mob) {
        return mob.getBehaviorController().getMemory(MemoryModuleType.NEAREST_BED);
    }

    private boolean tiredOfWalking(WorldServer world, EntityInsentient mob) {
        return !this.onOrOverBed(world, mob) && this.remainingTimeToReachBed <= 0;
    }

    private boolean tiredOfJumping(WorldServer world, EntityInsentient mob) {
        return this.onOrOverBed(world, mob) && this.remainingJumps <= 0;
    }
}
