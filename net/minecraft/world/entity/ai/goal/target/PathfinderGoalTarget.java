package net.minecraft.world.entity.ai.goal.target;

import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.scores.ScoreboardTeamBase;

public abstract class PathfinderGoalTarget extends PathfinderGoal {
    private static final int EMPTY_REACH_CACHE = 0;
    private static final int CAN_REACH_CACHE = 1;
    private static final int CANT_REACH_CACHE = 2;
    protected final EntityInsentient mob;
    protected final boolean mustSee;
    private final boolean mustReach;
    private int reachCache;
    private int reachCacheTime;
    private int unseenTicks;
    protected EntityLiving targetMob;
    protected int unseenMemoryTicks = 60;

    public PathfinderGoalTarget(EntityInsentient mob, boolean checkVisibility) {
        this(mob, checkVisibility, false);
    }

    public PathfinderGoalTarget(EntityInsentient mob, boolean checkVisibility, boolean checkNavigable) {
        this.mob = mob;
        this.mustSee = checkVisibility;
        this.mustReach = checkNavigable;
    }

    @Override
    public boolean canContinueToUse() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity == null) {
            livingEntity = this.targetMob;
        }

        if (livingEntity == null) {
            return false;
        } else if (!this.mob.canAttack(livingEntity)) {
            return false;
        } else {
            ScoreboardTeamBase team = this.mob.getScoreboardTeam();
            ScoreboardTeamBase team2 = livingEntity.getScoreboardTeam();
            if (team != null && team2 == team) {
                return false;
            } else {
                double d = this.getFollowDistance();
                if (this.mob.distanceToSqr(livingEntity) > d * d) {
                    return false;
                } else {
                    if (this.mustSee) {
                        if (this.mob.getEntitySenses().hasLineOfSight(livingEntity)) {
                            this.unseenTicks = 0;
                        } else if (++this.unseenTicks > this.unseenMemoryTicks) {
                            return false;
                        }
                    }

                    this.mob.setGoalTarget(livingEntity);
                    return true;
                }
            }
        }
    }

    protected double getFollowDistance() {
        return this.mob.getAttributeValue(GenericAttributes.FOLLOW_RANGE);
    }

    @Override
    public void start() {
        this.reachCache = 0;
        this.reachCacheTime = 0;
        this.unseenTicks = 0;
    }

    @Override
    public void stop() {
        this.mob.setGoalTarget((EntityLiving)null);
        this.targetMob = null;
    }

    protected boolean canAttack(@Nullable EntityLiving target, PathfinderTargetCondition targetPredicate) {
        if (target == null) {
            return false;
        } else if (!targetPredicate.test(this.mob, target)) {
            return false;
        } else if (!this.mob.isWithinRestriction(target.getChunkCoordinates())) {
            return false;
        } else {
            if (this.mustReach) {
                if (--this.reachCacheTime <= 0) {
                    this.reachCache = 0;
                }

                if (this.reachCache == 0) {
                    this.reachCache = this.canReach(target) ? 1 : 2;
                }

                if (this.reachCache == 2) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean canReach(EntityLiving entity) {
        this.reachCacheTime = 10 + this.mob.getRandom().nextInt(5);
        PathEntity path = this.mob.getNavigation().createPath(entity, 0);
        if (path == null) {
            return false;
        } else {
            PathPoint node = path.getEndNode();
            if (node == null) {
                return false;
            } else {
                int i = node.x - entity.getBlockX();
                int j = node.z - entity.getBlockZ();
                return (double)(i * i + j * j) <= 2.25D;
            }
        }
    }

    public PathfinderGoalTarget setUnseenMemoryTicks(int time) {
        this.unseenMemoryTicks = time;
        return this;
    }
}
