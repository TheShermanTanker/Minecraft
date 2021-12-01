package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;

public class PathfinderGoalLookAtPlayer extends PathfinderGoal {
    public static final float DEFAULT_PROBABILITY = 0.02F;
    protected final EntityInsentient mob;
    @Nullable
    protected Entity lookAt;
    protected final float lookDistance;
    private int lookTime;
    protected final float probability;
    private final boolean onlyHorizontal;
    protected final Class<? extends EntityLiving> lookAtType;
    protected final PathfinderTargetCondition lookAtContext;

    public PathfinderGoalLookAtPlayer(EntityInsentient mob, Class<? extends EntityLiving> targetType, float range) {
        this(mob, targetType, range, 0.02F);
    }

    public PathfinderGoalLookAtPlayer(EntityInsentient mob, Class<? extends EntityLiving> targetType, float range, float chance) {
        this(mob, targetType, range, chance, false);
    }

    public PathfinderGoalLookAtPlayer(EntityInsentient mob, Class<? extends EntityLiving> targetType, float range, float chance, boolean bl) {
        this.mob = mob;
        this.lookAtType = targetType;
        this.lookDistance = range;
        this.probability = chance;
        this.onlyHorizontal = bl;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.LOOK));
        if (targetType == EntityHuman.class) {
            this.lookAtContext = PathfinderTargetCondition.forNonCombat().range((double)range).selector((entity) -> {
                return IEntitySelector.notRiding(mob).test(entity);
            });
        } else {
            this.lookAtContext = PathfinderTargetCondition.forNonCombat().range((double)range);
        }

    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextFloat() >= this.probability) {
            return false;
        } else {
            if (this.mob.getGoalTarget() != null) {
                this.lookAt = this.mob.getGoalTarget();
            }

            if (this.lookAtType == EntityHuman.class) {
                this.lookAt = this.mob.level.getNearestPlayer(this.lookAtContext, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
            } else {
                this.lookAt = this.mob.level.getNearestEntity(this.mob.level.getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().grow((double)this.lookDistance, 3.0D, (double)this.lookDistance), (livingEntity) -> {
                    return true;
                }), this.lookAtContext, this.mob, this.mob.locX(), this.mob.getHeadY(), this.mob.locZ());
            }

            return this.lookAt != null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.lookAt.isAlive()) {
            return false;
        } else if (this.mob.distanceToSqr(this.lookAt) > (double)(this.lookDistance * this.lookDistance)) {
            return false;
        } else {
            return this.lookTime > 0;
        }
    }

    @Override
    public void start() {
        this.lookTime = this.adjustedTickDelay(40 + this.mob.getRandom().nextInt(40));
    }

    @Override
    public void stop() {
        this.lookAt = null;
    }

    @Override
    public void tick() {
        if (this.lookAt.isAlive()) {
            double d = this.onlyHorizontal ? this.mob.getHeadY() : this.lookAt.getHeadY();
            this.mob.getControllerLook().setLookAt(this.lookAt.locX(), d, this.lookAt.locZ());
            --this.lookTime;
        }
    }
}
