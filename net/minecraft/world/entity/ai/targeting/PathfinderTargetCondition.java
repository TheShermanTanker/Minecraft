package net.minecraft.world.entity.ai.targeting;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;

public class PathfinderTargetCondition {
    public static final PathfinderTargetCondition DEFAULT = forCombat();
    private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0D;
    private final boolean isCombat;
    private double range = -1.0D;
    private boolean checkLineOfSight = true;
    private boolean testInvisible = true;
    @Nullable
    private Predicate<EntityLiving> selector;

    private PathfinderTargetCondition(boolean attackable) {
        this.isCombat = attackable;
    }

    public static PathfinderTargetCondition forCombat() {
        return new PathfinderTargetCondition(true);
    }

    public static PathfinderTargetCondition forNonCombat() {
        return new PathfinderTargetCondition(false);
    }

    public PathfinderTargetCondition copy() {
        PathfinderTargetCondition targetingConditions = this.isCombat ? forCombat() : forNonCombat();
        targetingConditions.range = this.range;
        targetingConditions.checkLineOfSight = this.checkLineOfSight;
        targetingConditions.testInvisible = this.testInvisible;
        targetingConditions.selector = this.selector;
        return targetingConditions;
    }

    public PathfinderTargetCondition range(double baseMaxDistance) {
        this.range = baseMaxDistance;
        return this;
    }

    public PathfinderTargetCondition ignoreLineOfSight() {
        this.checkLineOfSight = false;
        return this;
    }

    public PathfinderTargetCondition ignoreInvisibilityTesting() {
        this.testInvisible = false;
        return this;
    }

    public PathfinderTargetCondition selector(@Nullable Predicate<EntityLiving> predicate) {
        this.selector = predicate;
        return this;
    }

    public boolean test(@Nullable EntityLiving baseEntity, EntityLiving targetEntity) {
        if (baseEntity == targetEntity) {
            return false;
        } else if (!targetEntity.canBeSeenByAnyone()) {
            return false;
        } else if (this.selector != null && !this.selector.test(targetEntity)) {
            return false;
        } else {
            if (baseEntity == null) {
                if (this.isCombat && (!targetEntity.canBeSeenAsEnemy() || targetEntity.level.getDifficulty() == EnumDifficulty.PEACEFUL)) {
                    return false;
                }
            } else {
                if (this.isCombat && (!baseEntity.canAttack(targetEntity) || !baseEntity.canAttackType(targetEntity.getEntityType()) || baseEntity.isAlliedTo(targetEntity))) {
                    return false;
                }

                if (this.range > 0.0D) {
                    double d = this.testInvisible ? targetEntity.getVisibilityPercent(baseEntity) : 1.0D;
                    double e = Math.max(this.range * d, 2.0D);
                    double f = baseEntity.distanceToSqr(targetEntity.locX(), targetEntity.locY(), targetEntity.locZ());
                    if (f > e * e) {
                        return false;
                    }
                }

                if (this.checkLineOfSight && baseEntity instanceof EntityInsentient) {
                    EntityInsentient mob = (EntityInsentient)baseEntity;
                    if (!mob.getEntitySenses().hasLineOfSight(targetEntity)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
