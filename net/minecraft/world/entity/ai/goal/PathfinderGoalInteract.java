package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;

public class PathfinderGoalInteract extends PathfinderGoalLookAtPlayer {
    public PathfinderGoalInteract(EntityInsentient mob, Class<? extends EntityLiving> targetType, float range) {
        super(mob, targetType, range);
        this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK, PathfinderGoal.Type.MOVE));
    }

    public PathfinderGoalInteract(EntityInsentient mob, Class<? extends EntityLiving> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK, PathfinderGoal.Type.MOVE));
    }
}
