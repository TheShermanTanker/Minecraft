package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;

public class PathfinderGoalOwnerHurtTarget extends PathfinderGoalTarget {
    private final EntityTameableAnimal tameAnimal;
    private EntityLiving ownerLastHurt;
    private int timestamp;

    public PathfinderGoalOwnerHurtTarget(EntityTameableAnimal tameable) {
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
                this.ownerLastHurt = livingEntity.getLastHurtMob();
                int i = livingEntity.getLastHurtMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurt, PathfinderTargetCondition.DEFAULT) && this.tameAnimal.wantsToAttack(this.ownerLastHurt, livingEntity);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setGoalTarget(this.ownerLastHurt);
        EntityLiving livingEntity = this.tameAnimal.getOwner();
        if (livingEntity != null) {
            this.timestamp = livingEntity.getLastHurtMobTimestamp();
        }

        super.start();
    }
}
