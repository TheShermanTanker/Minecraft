package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class PathfinderGoalBeg extends PathfinderGoal {
    private final EntityWolf wolf;
    @Nullable
    private EntityHuman player;
    private final World level;
    private final float lookDistance;
    private int lookTime;
    private final PathfinderTargetCondition begTargeting;

    public PathfinderGoalBeg(EntityWolf wolf, float begDistance) {
        this.wolf = wolf;
        this.level = wolf.level;
        this.lookDistance = begDistance;
        this.begTargeting = PathfinderTargetCondition.forNonCombat().range((double)begDistance);
        this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        this.player = this.level.getNearestPlayer(this.begTargeting, this.wolf);
        return this.player == null ? false : this.playerHoldingInteresting(this.player);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.player.isAlive()) {
            return false;
        } else if (this.wolf.distanceToSqr(this.player) > (double)(this.lookDistance * this.lookDistance)) {
            return false;
        } else {
            return this.lookTime > 0 && this.playerHoldingInteresting(this.player);
        }
    }

    @Override
    public void start() {
        this.wolf.setIsInterested(true);
        this.lookTime = this.adjustedTickDelay(40 + this.wolf.getRandom().nextInt(40));
    }

    @Override
    public void stop() {
        this.wolf.setIsInterested(false);
        this.player = null;
    }

    @Override
    public void tick() {
        this.wolf.getControllerLook().setLookAt(this.player.locX(), this.player.getHeadY(), this.player.locZ(), 10.0F, (float)this.wolf.getMaxHeadXRot());
        --this.lookTime;
    }

    private boolean playerHoldingInteresting(EntityHuman player) {
        for(EnumHand interactionHand : EnumHand.values()) {
            ItemStack itemStack = player.getItemInHand(interactionHand);
            if (this.wolf.isTamed() && itemStack.is(Items.BONE)) {
                return true;
            }

            if (this.wolf.isBreedItem(itemStack)) {
                return true;
            }
        }

        return false;
    }
}
