package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AxisAlignedBB;

public class PathfinderGoalHurtByTarget extends PathfinderGoalTarget {
    private static final PathfinderTargetCondition HURT_BY_TARGETING = PathfinderTargetCondition.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
    private static final int ALERT_RANGE_Y = 10;
    private boolean alertSameType;
    private int timestamp;
    private final Class<?>[] toIgnoreDamage;
    private Class<?>[] toIgnoreAlert;

    public PathfinderGoalHurtByTarget(EntityCreature mob, Class<?>... noRevengeTypes) {
        super(mob, true);
        this.toIgnoreDamage = noRevengeTypes;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.TARGET));
    }

    @Override
    public boolean canUse() {
        int i = this.mob.getLastHurtByMobTimestamp();
        EntityLiving livingEntity = this.mob.getLastDamager();
        if (i != this.timestamp && livingEntity != null) {
            if (livingEntity.getEntityType() == EntityTypes.PLAYER && this.mob.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                return false;
            } else {
                for(Class<?> class_ : this.toIgnoreDamage) {
                    if (class_.isAssignableFrom(livingEntity.getClass())) {
                        return false;
                    }
                }

                return this.canAttack(livingEntity, HURT_BY_TARGETING);
            }
        } else {
            return false;
        }
    }

    public PathfinderGoalHurtByTarget setAlertOthers(Class<?>... noHelpTypes) {
        this.alertSameType = true;
        this.toIgnoreAlert = noHelpTypes;
        return this;
    }

    @Override
    public void start() {
        this.mob.setGoalTarget(this.mob.getLastDamager());
        this.targetMob = this.mob.getGoalTarget();
        this.timestamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        if (this.alertSameType) {
            this.alertOthers();
        }

        super.start();
    }

    protected void alertOthers() {
        double d = this.getFollowDistance();
        AxisAlignedBB aABB = AxisAlignedBB.unitCubeFromLowerCorner(this.mob.getPositionVector()).grow(d, 10.0D, d);
        List<? extends EntityInsentient> list = this.mob.level.getEntitiesOfClass(this.mob.getClass(), aABB, IEntitySelector.NO_SPECTATORS);
        Iterator var5 = list.iterator();

        while(true) {
            EntityInsentient mob;
            while(true) {
                if (!var5.hasNext()) {
                    return;
                }

                mob = (EntityInsentient)var5.next();
                if (this.mob != mob && mob.getGoalTarget() == null && (!(this.mob instanceof EntityTameableAnimal) || ((EntityTameableAnimal)this.mob).getOwner() == ((EntityTameableAnimal)mob).getOwner()) && !mob.isAlliedTo(this.mob.getLastDamager())) {
                    if (this.toIgnoreAlert == null) {
                        break;
                    }

                    boolean bl = false;

                    for(Class<?> class_ : this.toIgnoreAlert) {
                        if (mob.getClass() == class_) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        break;
                    }
                }
            }

            this.alertOther(mob, this.mob.getLastDamager());
        }
    }

    protected void alertOther(EntityInsentient mob, EntityLiving target) {
        mob.setGoalTarget(target);
    }
}
