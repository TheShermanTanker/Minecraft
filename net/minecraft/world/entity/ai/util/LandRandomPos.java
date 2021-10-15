package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class LandRandomPos {
    @Nullable
    public static Vec3D getPos(EntityCreature entity, int horizontalRange, int verticalRange) {
        return getPos(entity, horizontalRange, verticalRange, entity::getWalkTargetValue);
    }

    @Nullable
    public static Vec3D getPos(EntityCreature entity, int horizontalRange, int verticalRange, ToDoubleFunction<BlockPosition> scorer) {
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(() -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirection(entity.getRandom(), horizontalRange, verticalRange);
            BlockPosition blockPos2 = generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
            return blockPos2 == null ? null : movePosUpOutOfSolid(entity, blockPos2);
        }, scorer);
    }

    @Nullable
    public static Vec3D getPosTowards(EntityCreature entity, int horizontalRange, int verticalRange, Vec3D end) {
        Vec3D vec3 = end.subtract(entity.locX(), entity.locY(), entity.locZ());
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return getPosInDirection(entity, horizontalRange, verticalRange, vec3, bl);
    }

    @Nullable
    public static Vec3D getPosAway(EntityCreature entity, int horizontalRange, int verticalRange, Vec3D start) {
        Vec3D vec3 = entity.getPositionVector().subtract(start);
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return getPosInDirection(entity, horizontalRange, verticalRange, vec3, bl);
    }

    @Nullable
    private static Vec3D getPosInDirection(EntityCreature entity, int horizontalRange, int verticalRange, Vec3D direction, boolean posTargetInRange) {
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, direction.x, direction.z, (double)((float)Math.PI / 2F));
            if (blockPos == null) {
                return null;
            } else {
                BlockPosition blockPos2 = generateRandomPosTowardDirection(entity, horizontalRange, posTargetInRange, blockPos);
                return blockPos2 == null ? null : movePosUpOutOfSolid(entity, blockPos2);
            }
        });
    }

    @Nullable
    public static BlockPosition movePosUpOutOfSolid(EntityCreature entity, BlockPosition pos) {
        pos = RandomPositionGenerator.moveUpOutOfSolid(pos, entity.level.getMaxBuildHeight(), (currentPos) -> {
            return PathfinderGoalUtil.isSolid(entity, currentPos);
        });
        return !PathfinderGoalUtil.isWater(entity, pos) && !PathfinderGoalUtil.hasMalus(entity, pos) ? pos : null;
    }

    @Nullable
    public static BlockPosition generateRandomPosTowardDirection(EntityCreature entity, int horizontalRange, boolean posTargetInRange, BlockPosition relativeInRangePos) {
        BlockPosition blockPos = RandomPositionGenerator.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), relativeInRangePos);
        return !PathfinderGoalUtil.isOutsideLimits(blockPos, entity) && !PathfinderGoalUtil.isRestricted(posTargetInRange, entity, blockPos) && !PathfinderGoalUtil.isNotStable(entity.getNavigation(), blockPos) ? blockPos : null;
    }
}
