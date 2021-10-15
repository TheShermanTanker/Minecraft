package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.IRangedEntity;

public class PathfinderGoalArrowAttack extends PathfinderGoal {
    private final EntityInsentient mob;
    private final IRangedEntity rangedAttackMob;
    private EntityLiving target;
    private int attackTime = -1;
    private final double speedModifier;
    private int seeTime;
    private final int attackIntervalMin;
    private final int attackIntervalMax;
    private final float attackRadius;
    private final float attackRadiusSqr;

    public PathfinderGoalArrowAttack(IRangedEntity mob, double mobSpeed, int intervalTicks, float maxShootRange) {
        this(mob, mobSpeed, intervalTicks, intervalTicks, maxShootRange);
    }

    public PathfinderGoalArrowAttack(IRangedEntity mob, double mobSpeed, int minIntervalTicks, int maxIntervalTicks, float maxShootRange) {
        if (!(mob instanceof EntityLiving)) {
            throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
        } else {
            this.rangedAttackMob = mob;
            this.mob = (EntityInsentient)mob;
            this.speedModifier = mobSpeed;
            this.attackIntervalMin = minIntervalTicks;
            this.attackIntervalMax = maxIntervalTicks;
            this.attackRadius = maxShootRange;
            this.attackRadiusSqr = maxShootRange * maxShootRange;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }
    }

    @Override
    public boolean canUse() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity != null && livingEntity.isAlive()) {
            this.target = livingEntity;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !this.mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
    }

    @Override
    public void tick() {
        double d = this.mob.distanceToSqr(this.target.locX(), this.target.locY(), this.target.locZ());
        boolean bl = this.mob.getEntitySenses().hasLineOfSight(this.target);
        if (bl) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (!(d > (double)this.attackRadiusSqr) && this.seeTime >= 5) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().moveTo(this.target, this.speedModifier);
        }

        this.mob.getControllerLook().setLookAt(this.target, 30.0F, 30.0F);
        if (--this.attackTime == 0) {
            if (!bl) {
                return;
            }

            float f = (float)Math.sqrt(d) / this.attackRadius;
            float g = MathHelper.clamp(f, 0.1F, 1.0F);
            this.rangedAttackMob.performRangedAttack(this.target, g);
            this.attackTime = MathHelper.floor(f * (float)(this.attackIntervalMax - this.attackIntervalMin) + (float)this.attackIntervalMin);
        } else if (this.attackTime < 0) {
            this.attackTime = MathHelper.floor(MathHelper.lerp(Math.sqrt(d) / (double)this.attackRadius, (double)this.attackIntervalMin, (double)this.attackIntervalMax));
        }

    }
}
