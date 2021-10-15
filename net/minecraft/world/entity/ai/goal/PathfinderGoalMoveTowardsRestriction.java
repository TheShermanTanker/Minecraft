package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalMoveTowardsRestriction extends PathfinderGoal {
    private final EntityCreature mob;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private final double speedModifier;

    public PathfinderGoalMoveTowardsRestriction(EntityCreature mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isWithinRestriction()) {
            return false;
        } else {
            Vec3D vec3 = DefaultRandomPos.getPosTowards(this.mob, 16, 7, Vec3D.atBottomCenterOf(this.mob.getRestrictCenter()), (double)((float)Math.PI / 2F));
            if (vec3 == null) {
                return false;
            } else {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;
                return true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }
}
