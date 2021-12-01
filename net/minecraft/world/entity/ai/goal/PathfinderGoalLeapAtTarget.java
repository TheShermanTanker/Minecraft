package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalLeapAtTarget extends PathfinderGoal {
    private final EntityInsentient mob;
    private EntityLiving target;
    private final float yd;

    public PathfinderGoalLeapAtTarget(EntityInsentient mob, float velocity) {
        this.mob = mob;
        this.yd = velocity;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isVehicle()) {
            return false;
        } else {
            this.target = this.mob.getGoalTarget();
            if (this.target == null) {
                return false;
            } else {
                double d = this.mob.distanceToSqr(this.target);
                if (!(d < 4.0D) && !(d > 16.0D)) {
                    if (!this.mob.isOnGround()) {
                        return false;
                    } else {
                        return this.mob.getRandom().nextInt(reducedTickDelay(5)) == 0;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.isOnGround();
    }

    @Override
    public void start() {
        Vec3D vec3 = this.mob.getMot();
        Vec3D vec32 = new Vec3D(this.target.locX() - this.mob.locX(), 0.0D, this.target.locZ() - this.mob.locZ());
        if (vec32.lengthSqr() > 1.0E-7D) {
            vec32 = vec32.normalize().scale(0.4D).add(vec3.scale(0.2D));
        }

        this.mob.setMot(vec32.x, (double)this.yd, vec32.z);
    }
}
