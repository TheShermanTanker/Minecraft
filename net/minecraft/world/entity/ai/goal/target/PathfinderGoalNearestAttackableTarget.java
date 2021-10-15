package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.AxisAlignedBB;

public class PathfinderGoalNearestAttackableTarget<T extends EntityLiving> extends PathfinderGoalTarget {
    protected final Class<T> targetType;
    protected final int randomInterval;
    protected EntityLiving target;
    protected PathfinderTargetCondition targetConditions;

    public PathfinderGoalNearestAttackableTarget(EntityInsentient mob, Class<T> targetClass, boolean checkVisibility) {
        this(mob, targetClass, checkVisibility, false);
    }

    public PathfinderGoalNearestAttackableTarget(EntityInsentient mob, Class<T> targetClass, boolean checkVisibility, boolean checkCanNavigate) {
        this(mob, targetClass, 10, checkVisibility, checkCanNavigate, (Predicate<EntityLiving>)null);
    }

    public PathfinderGoalNearestAttackableTarget(EntityInsentient mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<EntityLiving> targetPredicate) {
        super(mob, checkVisibility, checkCanNavigate);
        this.targetType = targetClass;
        this.randomInterval = reciprocalChance;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.TARGET));
        this.targetConditions = PathfinderTargetCondition.forCombat().range(this.getFollowDistance()).selector(targetPredicate);
    }

    @Override
    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        } else {
            this.findTarget();
            return this.target != null;
        }
    }

    protected AxisAlignedBB getTargetSearchArea(double distance) {
        return this.mob.getBoundingBox().grow(distance, 4.0D, distance);
    }

    protected void findTarget() {
        if (this.targetType != EntityHuman.class && this.targetType != EntityPlayer.class) {
            this.target = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), (livingEntity) -> {
                return true;
            }), this.targetConditions, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
        } else {
            this.target = this.mob.level.getNearestPlayer(this.targetConditions, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
        }

    }

    @Override
    public void start() {
        this.mob.setGoalTarget(this.target);
        super.start();
    }

    public void setTarget(@Nullable EntityLiving targetEntity) {
        this.target = targetEntity;
    }
}
