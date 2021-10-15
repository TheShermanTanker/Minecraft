package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.EntityInsentient;

public class PathfinderGoalFloat extends PathfinderGoal {
    private final EntityInsentient mob;

    public PathfinderGoalFloat(EntityInsentient mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP));
        mob.getNavigation().setCanFloat(true);
    }

    @Override
    public boolean canUse() {
        return this.mob.isInWater() && this.mob.getFluidHeight(TagsFluid.WATER) > this.mob.getFluidJumpThreshold() || this.mob.isInLava();
    }

    @Override
    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.getControllerJump().jump();
        }

    }
}
