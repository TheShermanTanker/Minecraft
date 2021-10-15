package net.minecraft.world.entity.ai.goal.target;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;

public class PathfinderGoalRandomTargetNonTamed<T extends EntityLiving> extends PathfinderGoalNearestAttackableTarget<T> {
    private final EntityTameableAnimal tamableMob;

    public PathfinderGoalRandomTargetNonTamed(EntityTameableAnimal tameable, Class<T> targetClass, boolean checkVisibility, @Nullable Predicate<EntityLiving> targetPredicate) {
        super(tameable, targetClass, 10, checkVisibility, false, targetPredicate);
        this.tamableMob = tameable;
    }

    @Override
    public boolean canUse() {
        return !this.tamableMob.isTamed() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetConditions != null ? this.targetConditions.test(this.mob, this.target) : super.canContinueToUse();
    }
}
