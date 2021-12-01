package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;

public class PathfinderGoalOcelotAttack extends PathfinderGoal {
    private final EntityInsentient mob;
    private EntityLiving target;
    private int attackTime;

    public PathfinderGoalOcelotAttack(EntityInsentient mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity == null) {
            return false;
        } else {
            this.target = livingEntity;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.target.isAlive()) {
            return false;
        } else if (this.mob.distanceToSqr(this.target) > 225.0D) {
            return false;
        } else {
            return !this.mob.getNavigation().isDone() || this.canUse();
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.mob.getControllerLook().setLookAt(this.target, 30.0F, 30.0F);
        double d = (double)(this.mob.getWidth() * 2.0F * this.mob.getWidth() * 2.0F);
        double e = this.mob.distanceToSqr(this.target.locX(), this.target.locY(), this.target.locZ());
        double f = 0.8D;
        if (e > d && e < 16.0D) {
            f = 1.33D;
        } else if (e < 225.0D) {
            f = 0.6D;
        }

        this.mob.getNavigation().moveTo(this.target, f);
        this.attackTime = Math.max(this.attackTime - 1, 0);
        if (!(e > d)) {
            if (this.attackTime <= 0) {
                this.attackTime = 20;
                this.mob.attackEntity(this.target);
            }
        }
    }
}
