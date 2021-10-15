package net.minecraft.world.entity.ai.goal.target;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.raid.EntityRaider;

public class PathfinderGoalNearestAttackableTargetWitch<T extends EntityLiving> extends PathfinderGoalNearestAttackableTarget<T> {
    private boolean canAttack = true;

    public PathfinderGoalNearestAttackableTargetWitch(EntityRaider actor, Class<T> targetEntityClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<EntityLiving> targetPredicate) {
        super(actor, targetEntityClass, reciprocalChance, checkVisibility, checkCanNavigate, targetPredicate);
    }

    public void setCanAttack(boolean enabled) {
        this.canAttack = enabled;
    }

    @Override
    public boolean canUse() {
        return this.canAttack && super.canUse();
    }
}
