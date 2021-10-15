package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class HoverRandomPos {
    @Nullable
    public static Vec3D getPos(EntityCreature entity, int horizontalRange, int verticalRange, double x, double z, float angle, int maxAboveSolid, int minAboveSolid) {
        boolean bl = PathfinderGoalUtil.mobRestricted(entity, horizontalRange);
        return RandomPositionGenerator.generateRandomPos(entity, () -> {
            BlockPosition blockPos = RandomPositionGenerator.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, x, z, (double)angle);
            if (blockPos == null) {
                return null;
            } else {
                BlockPosition blockPos2 = LandRandomPos.generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
                if (blockPos2 == null) {
                    return null;
                } else {
                    blockPos2 = RandomPositionGenerator.moveUpToAboveSolid(blockPos2, entity.getRandom().nextInt(maxAboveSolid - minAboveSolid + 1) + minAboveSolid, entity.level.getMaxBuildHeight(), (pos) -> {
                        return PathfinderGoalUtil.isSolid(entity, pos);
                    });
                    return !PathfinderGoalUtil.isWater(entity, blockPos2) && !PathfinderGoalUtil.hasMalus(entity, blockPos2) ? blockPos2 : null;
                }
            }
        });
    }
}
