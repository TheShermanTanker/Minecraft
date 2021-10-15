package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.pathfinder.PathEntity;

public class PathfinderGoalMeleeAttack extends PathfinderGoal {
    protected final EntityCreature mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private PathEntity path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private final int attackInterval = 20;
    private long lastCanUseCheck;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;

    public PathfinderGoalMeleeAttack(EntityCreature mob, double speed, boolean pauseWhenMobIdle) {
        this.mob = mob;
        this.speedModifier = speed;
        this.followingTargetEvenIfNotSeen = pauseWhenMobIdle;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        long l = this.mob.level.getTime();
        if (l - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = l;
            EntityLiving livingEntity = this.mob.getGoalTarget();
            if (livingEntity == null) {
                return false;
            } else if (!livingEntity.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().createPath(livingEntity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingEntity) >= this.mob.distanceToSqr(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ());
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity == null) {
            return false;
        } else if (!livingEntity.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else if (!this.mob.isWithinRestriction(livingEntity.getChunkCoordinates())) {
            return false;
        } else {
            return !(livingEntity instanceof EntityHuman) || !livingEntity.isSpectator() && !((EntityHuman)livingEntity).isCreative();
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (!IEntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
            this.mob.setGoalTarget((EntityLiving)null);
        }

        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        this.mob.getControllerLook().setLookAt(livingEntity, 30.0F, 30.0F);
        double d = this.mob.distanceToSqr(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ());
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if ((this.followingTargetEvenIfNotSeen || this.mob.getEntitySenses().hasLineOfSight(livingEntity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || livingEntity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.mob.getRandom().nextFloat() < 0.05F)) {
            this.pathedTargetX = livingEntity.locX();
            this.pathedTargetY = livingEntity.locY();
            this.pathedTargetZ = livingEntity.locZ();
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
            if (d > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (d > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            if (!this.mob.getNavigation().moveTo(livingEntity, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 15;
            }
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(livingEntity, d);
    }

    protected void checkAndPerformAttack(EntityLiving target, double squaredDistance) {
        double d = this.getAttackReachSqr(target);
        if (squaredDistance <= d && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swingHand(EnumHand.MAIN_HAND);
            this.mob.attackEntity(target);
        }

    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 20;
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    protected int getAttackInterval() {
        return 20;
    }

    protected double getAttackReachSqr(EntityLiving entity) {
        return (double)(this.mob.getWidth() * 2.0F * this.mob.getWidth() * 2.0F + entity.getWidth());
    }
}
