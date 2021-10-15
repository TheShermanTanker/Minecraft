package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class AirRandomPos {
    @Nullable
    public static Vec3D getPosTowards(EntityCreature entity, int horizontalRange, int verticalRange, int startHeight, Vec3D direction, double angleRange) {
        Vec3D vec3 = direction.subtract(entity.locX(), entity.locY(), entity.locZ());
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = AirAndWaterRandomPos.generateRandomPos(entity, horizontalRange, verticalRange, startHeight, vec3.x, vec3.z, angleRange, bl);
            return blockPos != null && !PathfinderGoalUtil.isWater(entity, blockPos) ? blockPos : null;
        });
    }
}
