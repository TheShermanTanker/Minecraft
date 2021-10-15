package net.minecraft.world.entity.ai.goal.target;

import java.util.List;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AxisAlignedBB;

public class PathfinderGoalUniversalAngerReset<T extends EntityInsentient & IEntityAngerable> extends PathfinderGoal {
    private static final int ALERT_RANGE_Y = 10;
    private final T mob;
    private final boolean alertOthersOfSameType;
    private int lastHurtByPlayerTimestamp;

    public PathfinderGoalUniversalAngerReset(T mob, boolean triggerOthers) {
        this.mob = mob;
        this.alertOthersOfSameType = triggerOthers;
    }

    @Override
    public boolean canUse() {
        return this.mob.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.wasHurtByPlayer();
    }

    private boolean wasHurtByPlayer() {
        return this.mob.getLastDamager() != null && this.mob.getLastDamager().getEntityType() == EntityTypes.PLAYER && this.mob.getLastHurtByMobTimestamp() > this.lastHurtByPlayerTimestamp;
    }

    @Override
    public void start() {
        this.lastHurtByPlayerTimestamp = this.mob.getLastHurtByMobTimestamp();
        this.mob.forgetCurrentTargetAndRefreshUniversalAnger();
        if (this.alertOthersOfSameType) {
            this.getNearbyMobsOfSameType().stream().filter((entity) -> {
                return entity != this.mob;
            }).map((entity) -> {
                return (IEntityAngerable)entity;
            }).forEach(IEntityAngerable::forgetCurrentTargetAndRefreshUniversalAnger);
        }

        super.start();
    }

    private List<? extends EntityInsentient> getNearbyMobsOfSameType() {
        double d = this.mob.getAttributeValue(GenericAttributes.FOLLOW_RANGE);
        AxisAlignedBB aABB = AxisAlignedBB.unitCubeFromLowerCorner(this.mob.getPositionVector()).grow(d, 10.0D, d);
        return this.mob.level.getEntitiesOfClass(this.mob.getClass(), aABB, IEntitySelector.NO_SPECTATORS);
    }
}
