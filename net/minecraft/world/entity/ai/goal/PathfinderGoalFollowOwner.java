package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfinderNormal;

public class PathfinderGoalFollowOwner extends PathfinderGoal {
    public static final int TELEPORT_WHEN_DISTANCE_IS = 12;
    private static final int MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 2;
    private static final int MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 3;
    private static final int MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 1;
    private final EntityTameableAnimal tamable;
    private EntityLiving owner;
    private final IWorldReader level;
    private final double speedModifier;
    private final NavigationAbstract navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;
    private final boolean canFly;

    public PathfinderGoalFollowOwner(EntityTameableAnimal tameable, double speed, float minDistance, float maxDistance, boolean leavesAllowed) {
        this.tamable = tameable;
        this.level = tameable.level;
        this.speedModifier = speed;
        this.navigation = tameable.getNavigation();
        this.startDistance = minDistance;
        this.stopDistance = maxDistance;
        this.canFly = leavesAllowed;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        if (!(tameable.getNavigation() instanceof Navigation) && !(tameable.getNavigation() instanceof NavigationFlying)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    @Override
    public boolean canUse() {
        EntityLiving livingEntity = this.tamable.getOwner();
        if (livingEntity == null) {
            return false;
        } else if (livingEntity.isSpectator()) {
            return false;
        } else if (this.tamable.isWillSit()) {
            return false;
        } else if (this.tamable.distanceToSqr(livingEntity) < (double)(this.startDistance * this.startDistance)) {
            return false;
        } else {
            this.owner = livingEntity;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (this.tamable.isWillSit()) {
            return false;
        } else {
            return !(this.tamable.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance));
        }
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.tamable.getPathfindingMalus(PathType.WATER);
        this.tamable.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.tamable.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.tamable.getControllerLook().setLookAt(this.owner, 10.0F, (float)this.tamable.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (!this.tamable.isLeashed() && !this.tamable.isPassenger()) {
                if (this.tamable.distanceToSqr(this.owner) >= 144.0D) {
                    this.teleportToOwner();
                } else {
                    this.navigation.moveTo(this.owner, this.speedModifier);
                }

            }
        }
    }

    private void teleportToOwner() {
        BlockPosition blockPos = this.owner.getChunkCoordinates();

        for(int i = 0; i < 10; ++i) {
            int j = this.randomIntInclusive(-3, 3);
            int k = this.randomIntInclusive(-1, 1);
            int l = this.randomIntInclusive(-3, 3);
            boolean bl = this.maybeTeleportTo(blockPos.getX() + j, blockPos.getY() + k, blockPos.getZ() + l);
            if (bl) {
                return;
            }
        }

    }

    private boolean maybeTeleportTo(int x, int y, int z) {
        if (Math.abs((double)x - this.owner.locX()) < 2.0D && Math.abs((double)z - this.owner.locZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPosition(x, y, z))) {
            return false;
        } else {
            this.tamable.setPositionRotation((double)x + 0.5D, (double)y, (double)z + 0.5D, this.tamable.getYRot(), this.tamable.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPosition pos) {
        PathType blockPathTypes = PathfinderNormal.getBlockPathTypeStatic(this.level, pos.mutable());
        if (blockPathTypes != PathType.WALKABLE) {
            return false;
        } else {
            IBlockData blockState = this.level.getType(pos.below());
            if (!this.canFly && blockState.getBlock() instanceof BlockLeaves) {
                return false;
            } else {
                BlockPosition blockPos = pos.subtract(this.tamable.getChunkCoordinates());
                return this.level.getCubes(this.tamable, this.tamable.getBoundingBox().move(blockPos));
            }
        }
    }

    private int randomIntInclusive(int min, int max) {
        return this.tamable.getRandom().nextInt(max - min + 1) + min;
    }
}
