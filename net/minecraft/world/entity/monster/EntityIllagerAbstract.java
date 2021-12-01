package net.minecraft.world.entity.monster;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.ai.goal.PathfinderGoalDoorOpen;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.level.World;

public abstract class EntityIllagerAbstract extends EntityRaider {
    protected EntityIllagerAbstract(EntityTypes<? extends EntityIllagerAbstract> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.ILLAGER;
    }

    public EntityIllagerAbstract.IllagerArmPose getArmPose() {
        return EntityIllagerAbstract.IllagerArmPose.CROSSED;
    }

    @Override
    public boolean canAttack(EntityLiving target) {
        return target instanceof EntityVillagerAbstract && target.isBaby() ? false : super.canAttack(target);
    }

    public static enum IllagerArmPose {
        CROSSED,
        ATTACKING,
        SPELLCASTING,
        BOW_AND_ARROW,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        CELEBRATING,
        NEUTRAL;
    }

    protected class RaiderOpenDoorGoal extends PathfinderGoalDoorOpen {
        public RaiderOpenDoorGoal(EntityRaider raider) {
            super(raider, false);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && EntityIllagerAbstract.this.hasActiveRaid();
        }
    }
}
