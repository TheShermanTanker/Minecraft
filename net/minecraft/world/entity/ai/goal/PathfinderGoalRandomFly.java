package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalRandomFly extends PathfinderGoalRandomStrollLand {
    public PathfinderGoalRandomFly(EntityCreature mob, double speed) {
        super(mob, speed);
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        Vec3D vec3 = this.mob.getViewVector(0.0F);
        int i = 8;
        Vec3D vec32 = HoverRandomPos.getPos(this.mob, 8, 7, vec3.x, vec3.z, ((float)Math.PI / 2F), 3, 1);
        return vec32 != null ? vec32 : AirAndWaterRandomPos.getPos(this.mob, 8, 4, -2, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
    }
}
