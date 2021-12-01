package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityInsentient;

public class PathfinderGoalRandomLookaround extends PathfinderGoal {
    private final EntityInsentient mob;
    private double relX;
    private double relZ;
    private int lookTime;

    public PathfinderGoalRandomLookaround(EntityInsentient mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getRandom().nextFloat() < 0.02F;
    }

    @Override
    public boolean canContinueToUse() {
        return this.lookTime >= 0;
    }

    @Override
    public void start() {
        double d = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
        this.relX = Math.cos(d);
        this.relZ = Math.sin(d);
        this.lookTime = 20 + this.mob.getRandom().nextInt(20);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        --this.lookTime;
        this.mob.getControllerLook().setLookAt(this.mob.locX() + this.relX, this.mob.getHeadY(), this.mob.locZ() + this.relZ);
    }
}
