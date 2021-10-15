package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;

public class PathfinderGoalOwnerHurtByTarget extends PathfinderGoalTarget {
    private final EntityTameableAnimal tameAnimal;
    private EntityLiving ownerLastHurtBy;
    private int timestamp;

    public PathfinderGoalOwnerHurtByTarget(EntityTameableAnimal tameable) {
        super(tameable, false);
        this.tameAnimal = tameable;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.tameAnimal.isTamed() && !this.tameAnimal.isWillSit()) {
            EntityLiving livingEntity = this.tameAnimal.getOwner();
            if (livingEntity == null) {
                return false;
            } else {
                this.ownerLastHurtBy = livingEntity.getLastDamager();
                int i = livingEntity.getLastHurtByMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, PathfinderTargetCondition.DEFAULT) && this.tameAnimal.wantsToAttack(this.ownerLastHurtBy, livingEntity);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setGoalTarget(this.ownerLastHurtBy);
        EntityLiving livingEntity = this.tameAnimal.getOwner();
        if (livingEntity != null) {
            this.timestamp = livingEntity.getLastHurtByMobTimestamp();
        }

        super.start();
    }
}
