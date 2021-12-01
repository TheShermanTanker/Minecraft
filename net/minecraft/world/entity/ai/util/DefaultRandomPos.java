package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class DefaultRandomPos {
    @Nullable
    public static Vec3D getPos(EntityCreature entity, int horizontalRange, int verticalRange) {
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirection(entity.getRandom(), horizontalRange, verticalRange);
            return generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    public static Vec3D getPosTowards(EntityCreature entity, int horizontalRange, int verticalRange, Vec3D end, double angleRange) {
        Vec3D vec3 = end.subtract(entity.locX(), entity.locY(), entity.locZ());
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, vec3.x, vec3.z, angleRange);
            return blockPos == null ? null : generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    public static Vec3D getPosAway(EntityCreature entity, int horizontalRange, int verticalRange, Vec3D start) {
        Vec3D vec3 = entity.getPositionVector().subtract(start);
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
            return blockPos == null ? null : generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    private static BlockPosition generateRandomPosTowardDirection(EntityCreature entity, int horizontalRange, boolean posTargetInRange, BlockPosition fuzz) {
        BlockPosition blockPos = RandomPositionGenerator.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), fuzz);
        return !PathfinderGoalUtil.isOutsideLimits(blockPos, entity) && !PathfinderGoalUtil.isRestricted(posTargetInRange, entity, blockPos) && !PathfinderGoalUtil.isNotStable(entity.getNavigation(), blockPos) && !PathfinderGoalUtil.hasMalus(entity, blockPos) ? blockPos : null;
    }
}
