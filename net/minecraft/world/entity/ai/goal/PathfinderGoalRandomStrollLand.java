package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRandomStrollLand extends PathfinderGoalRandomStroll {
    public static final float PROBABILITY = 0.001F;
    protected final float probability;

    public PathfinderGoalRandomStrollLand(EntityCreature mob, double speed) {
        this(mob, speed, 0.001F);
    }

    public PathfinderGoalRandomStrollLand(EntityCreature mob, double speed, float probability) {
        super(mob, speed);
        this.probability = probability;
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        if (this.mob.isInWaterOrBubble()) {
            Vec3D vec3 = LandRandomPos.getPos(this.mob, 15, 7);
            return vec3 == null ? super.getPosition() : vec3;
        } else {
            return this.mob.getRandom().nextFloat() >= this.probability ? LandRandomPos.getPos(this.mob, 10, 7) : super.getPosition();
        }
    }
}
