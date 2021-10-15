package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.Vec3D;

public class PrepareRamNearestTarget<E extends EntityCreature> extends Behavior<E> {
    public static final int TIME_OUT_DURATION = 160;
    private final ToIntFunction<E> getCooldownOnFail;
    private final int minRamDistance;
    private final int maxRamDistance;
    private final float walkSpeed;
    private final PathfinderTargetCondition ramTargeting;
    private final int ramPrepareTime;
    private final Function<E, SoundEffect> getPrepareRamSound;
    private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
    private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

    public PrepareRamNearestTarget(ToIntFunction<E> cooldownFactory, int minDistance, int maxDistance, float speed, PathfinderTargetCondition targetPredicate, int prepareTime, Function<E, SoundEffect> soundFactory) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_ABSENT), 160);
        this.getCooldownOnFail = cooldownFactory;
        this.minRamDistance = minDistance;
        this.maxRamDistance = maxDistance;
        this.walkSpeed = speed;
        this.ramTargeting = targetPredicate;
        this.ramPrepareTime = prepareTime;
        this.getPrepareRamSound = soundFactory;
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((mobs) -> {
            return mobs.stream().filter((mob) -> {
                return this.ramTargeting.test(entity, mob);
            }).findFirst();
        }).ifPresent((mob) -> {
            this.chooseRamPosition(entity, mob);
        });
    }

    @Override
    protected void stop(WorldServer serverLevel, E pathfinderMob, long l) {
        BehaviorController<?> brain = pathfinderMob.getBehaviorController();
        if (!brain.hasMemory(MemoryModuleType.RAM_TARGET)) {
            serverLevel.broadcastEntityEffect(pathfinderMob, (byte)59);
            brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(pathfinderMob));
        }

    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
    }

    @Override
    protected void tick(WorldServer world, E entity, long time) {
        if (this.ramCandidate.isPresent()) {
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
            entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(this.ramCandidate.get().getTarget(), true));
            boolean bl = !this.ramCandidate.get().getTarget().getChunkCoordinates().equals(this.ramCandidate.get().getTargetPosition());
            if (bl) {
                world.broadcastEntityEffect(entity, (byte)59);
                entity.getNavigation().stop();
                this.chooseRamPosition(entity, (this.ramCandidate.get()).target);
            } else {
                BlockPosition blockPos = entity.getChunkCoordinates();
                if (blockPos.equals(this.ramCandidate.get().getStartPosition())) {
                    world.broadcastEntityEffect(entity, (byte)58);
                    if (!this.reachedRamPositionTimestamp.isPresent()) {
                        this.reachedRamPositionTimestamp = Optional.of(time);
                    }

                    if (time - this.reachedRamPositionTimestamp.get() >= (long)this.ramPrepareTime) {
                        entity.getBehaviorController().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(blockPos, this.ramCandidate.get().getTargetPosition()));
                        world.playSound((EntityHuman)null, entity, this.getPrepareRamSound.apply(entity), SoundCategory.HOSTILE, 1.0F, entity.getVoicePitch());
                        this.ramCandidate = Optional.empty();
                    }
                }
            }

        }
    }

    private Vec3D getEdgeOfBlock(BlockPosition start, BlockPosition end) {
        double d = 0.5D;
        double e = 0.5D * (double)MathHelper.sign((double)(end.getX() - start.getX()));
        double f = 0.5D * (double)MathHelper.sign((double)(end.getZ() - start.getZ()));
        return Vec3D.atBottomCenterOf(end).add(e, 0.0D, f);
    }

    private Optional<BlockPosition> calculateRammingStartPosition(EntityCreature entity, EntityLiving target) {
        BlockPosition blockPos = target.getChunkCoordinates();
        if (!this.isWalkableBlock(entity, blockPos)) {
            return Optional.empty();
        } else {
            List<BlockPosition> list = Lists.newArrayList();
            BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                mutableBlockPos.set(blockPos);

                for(int i = 0; i < this.maxRamDistance; ++i) {
                    if (!this.isWalkableBlock(entity, mutableBlockPos.move(direction))) {
                        mutableBlockPos.move(direction.opposite());
                        break;
                    }
                }

                if (mutableBlockPos.distManhattan(blockPos) >= this.minRamDistance) {
                    list.add(mutableBlockPos.immutableCopy());
                }
            }

            NavigationAbstract pathNavigation = entity.getNavigation();
            return list.stream().sorted(Comparator.comparingDouble(entity.getChunkCoordinates()::distSqr)).filter((start) -> {
                PathEntity path = pathNavigation.createPath(start, 0);
                return path != null && path.canReach();
            }).findFirst();
        }
    }

    private boolean isWalkableBlock(EntityCreature entity, BlockPosition target) {
        return entity.getNavigation().isStableDestination(target) && entity.getPathfindingMalus(PathfinderNormal.getBlockPathTypeStatic(entity.level, target.mutable())) == 0.0F;
    }

    private void chooseRamPosition(EntityCreature entity, EntityLiving target) {
        this.reachedRamPositionTimestamp = Optional.empty();
        this.ramCandidate = this.calculateRammingStartPosition(entity, target).map((start) -> {
            return new PrepareRamNearestTarget.RamCandidate(start, target.getChunkCoordinates(), target);
        });
    }

    public static class RamCandidate {
        private final BlockPosition startPosition;
        private final BlockPosition targetPosition;
        final EntityLiving target;

        public RamCandidate(BlockPosition start, BlockPosition end, EntityLiving entity) {
            this.startPosition = start;
            this.targetPosition = end;
            this.target = entity;
        }

        public BlockPosition getStartPosition() {
            return this.startPosition;
        }

        public BlockPosition getTargetPosition() {
            return this.targetPosition;
        }

        public EntityLiving getTarget() {
            return this.target;
        }
    }
}
