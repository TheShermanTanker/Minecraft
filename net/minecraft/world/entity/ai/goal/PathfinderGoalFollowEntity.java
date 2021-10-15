package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.level.pathfinder.PathType;

public class PathfinderGoalFollowEntity extends PathfinderGoal {
    private final EntityInsentient mob;
    private final Predicate<EntityInsentient> followPredicate;
    private EntityInsentient followingMob;
    private final double speedModifier;
    private final NavigationAbstract navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private float oldWaterCost;
    private final float areaSize;

    public PathfinderGoalFollowEntity(EntityInsentient mob, double speed, float minDistance, float maxDistance) {
        this.mob = mob;
        this.followPredicate = (mob2) -> {
            return mob2 != null && mob.getClass() != mob2.getClass();
        };
        this.speedModifier = speed;
        this.navigation = mob.getNavigation();
        this.stopDistance = minDistance;
        this.areaSize = maxDistance;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        if (!(mob.getNavigation() instanceof Navigation) && !(mob.getNavigation() instanceof NavigationFlying)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowMobGoal");
        }
    }

    @Override
    public boolean canUse() {
        List<EntityInsentient> list = this.mob.level.getEntitiesOfClass(EntityInsentient.class, this.mob.getBoundingBox().inflate((double)this.areaSize), this.followPredicate);
        if (!list.isEmpty()) {
            for(EntityInsentient mob : list) {
                if (!mob.isInvisible()) {
                    this.followingMob = mob;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.followingMob != null && !this.navigation.isDone() && this.mob.distanceToSqr(this.followingMob) > (double)(this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.mob.getPathfindingMalus(PathType.WATER);
        this.mob.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.followingMob = null;
        this.navigation.stop();
        this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        if (this.followingMob != null && !this.mob.isLeashed()) {
            this.mob.getControllerLook().setLookAt(this.followingMob, 10.0F, (float)this.mob.getMaxHeadXRot());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                double d = this.mob.locX() - this.followingMob.locX();
                double e = this.mob.locY() - this.followingMob.locY();
                double f = this.mob.locZ() - this.followingMob.locZ();
                double g = d * d + e * e + f * f;
                if (!(g <= (double)(this.stopDistance * this.stopDistance))) {
                    this.navigation.moveTo(this.followingMob, this.speedModifier);
                } else {
                    this.navigation.stop();
                    ControllerLook lookControl = this.followingMob.getControllerLook();
                    if (g <= (double)this.stopDistance || lookControl.getWantedX() == this.mob.locX() && lookControl.getWantedY() == this.mob.locY() && lookControl.getWantedZ() == this.mob.locZ()) {
                        double h = this.followingMob.locX() - this.mob.locX();
                        double i = this.followingMob.locZ() - this.mob.locZ();
                        this.navigation.moveTo(this.mob.locX() - h, this.mob.locY(), this.mob.locZ() - i, this.speedModifier);
                    }

                }
            }
        }
    }
}
