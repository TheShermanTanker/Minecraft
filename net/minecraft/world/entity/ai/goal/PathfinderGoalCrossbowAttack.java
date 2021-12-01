package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.ICrossbow;
import net.minecraft.world.entity.monster.IRangedEntity;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.item.ItemCrossbow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PathfinderGoalCrossbowAttack<T extends EntityMonster & IRangedEntity & ICrossbow> extends PathfinderGoal {
    public static final IntProviderUniform PATHFINDING_DELAY_RANGE = TimeRange.rangeOfSeconds(1, 2);
    private final T mob;
    private PathfinderGoalCrossbowAttack.State crossbowState = PathfinderGoalCrossbowAttack.State.UNCHARGED;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public PathfinderGoalCrossbowAttack(T actor, double speed, float range) {
        this.mob = actor;
        this.speedModifier = speed;
        this.attackRadiusSqr = range * range;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(Items.CROSSBOW);
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getGoalTarget() != null && this.mob.getGoalTarget().isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setGoalTarget((EntityLiving)null);
        this.seeTime = 0;
        if (this.mob.isHandRaised()) {
            this.mob.clearActiveItem();
            this.mob.setChargingCrossbow(false);
            ItemCrossbow.setCharged(this.mob.getActiveItem(), false);
        }

    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity != null) {
            boolean bl = this.mob.getEntitySenses().hasLineOfSight(livingEntity);
            boolean bl2 = this.seeTime > 0;
            if (bl != bl2) {
                this.seeTime = 0;
            }

            if (bl) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            double d = this.mob.distanceToSqr(livingEntity);
            boolean bl3 = (d > (double)this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
            if (bl3) {
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().moveTo(livingEntity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.mob.getNavigation().stop();
            }

            this.mob.getControllerLook().setLookAt(livingEntity, 30.0F, 30.0F);
            if (this.crossbowState == PathfinderGoalCrossbowAttack.State.UNCHARGED) {
                if (!bl3) {
                    this.mob.startUsingItem(ProjectileHelper.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
                    this.crossbowState = PathfinderGoalCrossbowAttack.State.CHARGING;
                    this.mob.setChargingCrossbow(true);
                }
            } else if (this.crossbowState == PathfinderGoalCrossbowAttack.State.CHARGING) {
                if (!this.mob.isHandRaised()) {
                    this.crossbowState = PathfinderGoalCrossbowAttack.State.UNCHARGED;
                }

                int i = this.mob.getTicksUsingItem();
                ItemStack itemStack = this.mob.getActiveItem();
                if (i >= ItemCrossbow.getChargeDuration(itemStack)) {
                    this.mob.releaseActiveItem();
                    this.crossbowState = PathfinderGoalCrossbowAttack.State.CHARGED;
                    this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                    this.mob.setChargingCrossbow(false);
                }
            } else if (this.crossbowState == PathfinderGoalCrossbowAttack.State.CHARGED) {
                --this.attackDelay;
                if (this.attackDelay == 0) {
                    this.crossbowState = PathfinderGoalCrossbowAttack.State.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == PathfinderGoalCrossbowAttack.State.READY_TO_ATTACK && bl) {
                this.mob.performRangedAttack(livingEntity, 1.0F);
                ItemStack itemStack2 = this.mob.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
                ItemCrossbow.setCharged(itemStack2, false);
                this.crossbowState = PathfinderGoalCrossbowAttack.State.UNCHARGED;
            }

        }
    }

    private boolean canRun() {
        return this.crossbowState == PathfinderGoalCrossbowAttack.State.UNCHARGED;
    }

    static enum State {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }
}
