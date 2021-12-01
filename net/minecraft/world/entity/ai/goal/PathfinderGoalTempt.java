package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.crafting.RecipeItemStack;

public class PathfinderGoalTempt extends PathfinderGoal {
    private static final PathfinderTargetCondition TEMP_TARGETING = PathfinderTargetCondition.forNonCombat().range(10.0D).ignoreLineOfSight();
    private final PathfinderTargetCondition targetingConditions;
    protected final EntityCreature mob;
    private final double speedModifier;
    private double px;
    private double py;
    private double pz;
    private double pRotX;
    private double pRotY;
    @Nullable
    protected EntityHuman player;
    private int calmDown;
    private boolean isRunning;
    private final RecipeItemStack items;
    private final boolean canScare;

    public PathfinderGoalTempt(EntityCreature entity, double speed, RecipeItemStack food, boolean canBeScared) {
        this.mob = entity;
        this.speedModifier = speed;
        this.items = food;
        this.canScare = canBeScared;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
    }

    @Override
    public boolean canUse() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        } else {
            this.player = this.mob.level.getNearestPlayer(this.targetingConditions, this.mob);
            return this.player != null;
        }
    }

    private boolean shouldFollow(EntityLiving entity) {
        return this.items.test(entity.getItemInMainHand()) || this.items.test(entity.getItemInOffHand());
    }

    @Override
    public boolean canContinueToUse() {
        if (this.canScare()) {
            if (this.mob.distanceToSqr(this.player) < 36.0D) {
                if (this.player.distanceToSqr(this.px, this.py, this.pz) > 0.010000000000000002D) {
                    return false;
                }

                if (Math.abs((double)this.player.getXRot() - this.pRotX) > 5.0D || Math.abs((double)this.player.getYRot() - this.pRotY) > 5.0D) {
                    return false;
                }
            } else {
                this.px = this.player.locX();
                this.py = this.player.locY();
                this.pz = this.player.locZ();
            }

            this.pRotX = (double)this.player.getXRot();
            this.pRotY = (double)this.player.getYRot();
        }

        return this.canUse();
    }

    protected boolean canScare() {
        return this.canScare;
    }

    @Override
    public void start() {
        this.px = this.player.locX();
        this.py = this.player.locY();
        this.pz = this.player.locZ();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.player = null;
        this.mob.getNavigation().stop();
        this.calmDown = reducedTickDelay(100);
        this.isRunning = false;
    }

    @Override
    public void tick() {
        this.mob.getControllerLook().setLookAt(this.player, (float)(this.mob.getMaxHeadYRot() + 20), (float)this.mob.getMaxHeadXRot());
        if (this.mob.distanceToSqr(this.player) < 6.25D) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().moveTo(this.player, this.speedModifier);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
