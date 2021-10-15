package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class AirAndWaterRandomPos {
    @Nullable
    public static Vec3D getPos(EntityCreature entity, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double rangeAngle) {
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            return generateRandomPos(entity, horizontalRange, verticalRange, startHeight, directionX, directionZ, rangeAngle, bl);
        });
    }

    @Nullable
    public static BlockPosition generateRandomPos(EntityCreature entity, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double rangeAngle, boolean posTargetInRange) {
        BlockPosition blockPos = RandomPositionGenerator.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, startHeight, directionX, directionZ, rangeAngle);
        if (blockPos == null) {
            return null;
        } else {
            BlockPosition blockPos2 = RandomPositionGenerator.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), blockPos);
            if (!PathfinderGoalUtil.isOutsideLimits(blockPos2, entity) && !PathfinderGoalUtil.isRestricted(posTargetInRange, entity, blockPos2)) {
                blockPos2 = RandomPositionGenerator.moveUpOutOfSolid(blockPos2, entity.level.getMaxBuildHeight(), (pos) -> {
                    return PathfinderGoalUtil.isSolid(entity, pos);
                });
                return PathfinderGoalUtil.hasMalus(entity, blockPos2) ? null : blockPos2;
            } else {
                return null;
            }
        }
    }
}
