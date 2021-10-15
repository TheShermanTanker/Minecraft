package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;

public class PathfinderGoalSit extends PathfinderGoal {
    private final EntityTameableAnimal mob;

    public PathfinderGoalSit(EntityTameableAnimal tameable) {
        this.mob = tameable;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isWillSit();
    }

    @Override
    public boolean canUse() {
        if (!this.mob.isTamed()) {
            return false;
        } else if (this.mob.isInWaterOrBubble()) {
            return false;
        } else if (!this.mob.isOnGround()) {
            return false;
        } else {
            EntityLiving livingEntity = this.mob.getOwner();
            if (livingEntity == null) {
                return true;
            } else {
                return this.mob.distanceToSqr(livingEntity) < 144.0D && livingEntity.getLastDamager() != null ? false : this.mob.isWillSit();
            }
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.mob.setSitting(true);
    }

    @Override
    public void stop() {
        this.mob.setSitting(false);
    }
}
