package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRandomSwim extends PathfinderGoalRandomStroll {
    public PathfinderGoalRandomSwim(EntityCreature mob, double speed, int chance) {
        super(mob, speed, chance);
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        return BehaviorUtil.getRandomSwimmablePos(this.mob, 10, 7);
    }
}
