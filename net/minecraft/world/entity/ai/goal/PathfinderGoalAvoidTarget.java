package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalAvoidTarget<T extends EntityLiving> extends PathfinderGoal {
    protected final EntityCreature mob;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;
    @Nullable
    protected T toAvoid;
    protected final float maxDist;
    @Nullable
    protected PathEntity path;
    protected final NavigationAbstract pathNav;
    protected final Class<T> avoidClass;
    protected final Predicate<EntityLiving> avoidPredicate;
    protected final Predicate<EntityLiving> predicateOnAvoidEntity;
    private final PathfinderTargetCondition avoidEntityTargeting;

    public PathfinderGoalAvoidTarget(EntityCreature mob, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
        this(mob, fleeFromType, (livingEntity) -> {
            return true;
        }, distance, slowSpeed, fastSpeed, IEntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public PathfinderGoalAvoidTarget(EntityCreature mob, Class<T> fleeFromType, Predicate<EntityLiving> extraInclusionSelector, float distance, double slowSpeed, double fastSpeed, Predicate<EntityLiving> inclusionSelector) {
        this.mob = mob;
        this.avoidClass = fleeFromType;
        this.avoidPredicate = extraInclusionSelector;
        this.maxDist = distance;
        this.walkSpeedModifier = slowSpeed;
        this.sprintSpeedModifier = fastSpeed;
        this.predicateOnAvoidEntity = inclusionSelector;
        this.pathNav = mob.getNavigation();
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        this.avoidEntityTargeting = PathfinderTargetCondition.forCombat().range((double)distance).selector(inclusionSelector.and(extraInclusionSelector));
    }

    public PathfinderGoalAvoidTarget(EntityCreature fleeingEntity, Class<T> classToFleeFrom, float fleeDistance, double fleeSlowSpeed, double fleeFastSpeed, Predicate<EntityLiving> inclusionSelector) {
        this(fleeingEntity, classToFleeFrom, (livingEntity) -> {
            return true;
        }, fleeDistance, fleeSlowSpeed, fleeFastSpeed, inclusionSelector);
    }

    @Override
    public boolean canUse() {
        this.toAvoid = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.avoidClass, this.mob.getBoundingBox().grow((double)this.maxDist, 3.0D, (double)this.maxDist), (livingEntity) -> {
            return true;
        }), this.avoidEntityTargeting, this.mob, this.mob.locX(), this.mob.locY(), this.mob.locZ());
        if (this.toAvoid == null) {
            return false;
        } else {
            Vec3D vec3 = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.getPositionVector());
            if (vec3 == null) {
                return false;
            } else if (this.toAvoid.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.toAvoid.distanceToSqr(this.mob)) {
                return false;
            } else {
                this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
                return this.path != null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.pathNav.isDone();
    }

    @Override
    public void start() {
        this.pathNav.moveTo(this.path, this.walkSpeedModifier);
    }

    @Override
    public void stop() {
        this.toAvoid = null;
    }

    @Override
    public void tick() {
        if (this.mob.distanceToSqr(this.toAvoid) < 49.0D) {
            this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        } else {
            this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }

    }
}
