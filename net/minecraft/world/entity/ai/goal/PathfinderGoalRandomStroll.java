package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRandomStroll extends PathfinderGoal {
    public static final int DEFAULT_INTERVAL = 120;
    protected final EntityCreature mob;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected final double speedModifier;
    protected int interval;
    protected boolean forceTrigger;
    private final boolean checkNoActionTime;

    public PathfinderGoalRandomStroll(EntityCreature mob, double speed) {
        this(mob, speed, 120);
    }

    public PathfinderGoalRandomStroll(EntityCreature mob, double speed, int chance) {
        this(mob, speed, chance, true);
    }

    public PathfinderGoalRandomStroll(EntityCreature entity, double speed, int chance, boolean canDespawn) {
        this.mob = entity;
        this.speedModifier = speed;
        this.interval = chance;
        this.checkNoActionTime = canDespawn;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isVehicle()) {
            return false;
        } else {
            if (!this.forceTrigger) {
                if (this.checkNoActionTime && this.mob.getNoActionTime() >= 100) {
                    return false;
                }

                if (this.mob.getRandom().nextInt(reducedTickDelay(this.interval)) != 0) {
                    return false;
                }
            }

            Vec3D vec3 = this.getPosition();
            if (vec3 == null) {
                return false;
            } else {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;
                this.forceTrigger = false;
                return true;
            }
        }
    }

    @Nullable
    protected Vec3D getPosition() {
        return DefaultRandomPos.getPos(this.mob, 10, 7);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() && !this.mob.isVehicle();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        super.stop();
    }

    public void trigger() {
        this.forceTrigger = true;
    }

    public void setTimeBetweenMovement(int chance) {
        this.interval = chance;
    }
}
