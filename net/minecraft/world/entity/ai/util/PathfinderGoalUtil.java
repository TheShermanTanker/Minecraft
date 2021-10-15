package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.level.pathfinder.PathfinderNormal;

public class PathfinderGoalUtil {
    public static boolean hasGroundPathNavigation(EntityInsentient entity) {
        return entity.getNavigation() instanceof Navigation;
    }

    public static boolean mobRestricted(EntityCreature entity, int extraDistance) {
        return entity.hasRestriction() && entity.getRestrictCenter().closerThan(entity.getPositionVector(), (double)(entity.getRestrictRadius() + (float)extraDistance) + 1.0D);
    }

    public static boolean isOutsideLimits(BlockPosition pos, EntityCreature entity) {
        return pos.getY() < entity.level.getMinBuildHeight() || pos.getY() > entity.level.getMaxBuildHeight();
    }

    public static boolean isRestricted(boolean posTargetInRange, EntityCreature entity, BlockPosition pos) {
        return posTargetInRange && !entity.isWithinRestriction(pos);
    }

    public static boolean isNotStable(NavigationAbstract navigation, BlockPosition pos) {
        return !navigation.isStableDestination(pos);
    }

    public static boolean isWater(EntityCreature entity, BlockPosition pos) {
        return entity.level.getFluid(pos).is(TagsFluid.WATER);
    }

    public static boolean hasMalus(EntityCreature entity, BlockPosition pos) {
        return entity.getPathfindingMalus(PathfinderNormal.getBlockPathTypeStatic(entity.level, pos.mutable())) != 0.0F;
    }

    public static boolean isSolid(EntityCreature entity, BlockPosition pos) {
        return entity.level.getType(pos).getMaterial().isBuildable();
    }
}
