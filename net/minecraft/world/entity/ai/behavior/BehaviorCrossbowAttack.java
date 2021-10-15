package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.ICrossbow;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.item.ItemCrossbow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BehaviorCrossbowAttack<E extends EntityInsentient & ICrossbow, T extends EntityLiving> extends Behavior<E> {
    private static final int TIMEOUT = 1200;
    private int attackDelay;
    private BehaviorCrossbowAttack.BowState crossbowState = BehaviorCrossbowAttack.BowState.UNCHARGED;

    public BehaviorCrossbowAttack() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        EntityLiving livingEntity = getAttackTarget(entity);
        return entity.isHolding(Items.CROSSBOW) && BehaviorUtil.canSee(entity, livingEntity) && BehaviorUtil.isWithinAttackRange(entity, livingEntity, 0);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, E mob, long l) {
        return mob.getBehaviorController().hasMemory(MemoryModuleType.ATTACK_TARGET) && this.checkExtraStartConditions(serverLevel, mob);
    }

    @Override
    protected void tick(WorldServer serverLevel, E mob, long l) {
        EntityLiving livingEntity = getAttackTarget(mob);
        this.lookAtTarget(mob, livingEntity);
        this.crossbowAttack(mob, livingEntity);
    }

    @Override
    protected void stop(WorldServer world, E entity, long time) {
        if (entity.isHandRaised()) {
            entity.clearActiveItem();
        }

        if (entity.isHolding(Items.CROSSBOW)) {
            entity.setChargingCrossbow(false);
            ItemCrossbow.setCharged(entity.getActiveItem(), false);
        }

    }

    private void crossbowAttack(E entity, EntityLiving target) {
        if (this.crossbowState == BehaviorCrossbowAttack.BowState.UNCHARGED) {
            entity.startUsingItem(ProjectileHelper.getWeaponHoldingHand(entity, Items.CROSSBOW));
            this.crossbowState = BehaviorCrossbowAttack.BowState.CHARGING;
            entity.setChargingCrossbow(true);
        } else if (this.crossbowState == BehaviorCrossbowAttack.BowState.CHARGING) {
            if (!entity.isHandRaised()) {
                this.crossbowState = BehaviorCrossbowAttack.BowState.UNCHARGED;
            }

            int i = entity.getTicksUsingItem();
            ItemStack itemStack = entity.getActiveItem();
            if (i >= ItemCrossbow.getChargeDuration(itemStack)) {
                entity.releaseActiveItem();
                this.crossbowState = BehaviorCrossbowAttack.BowState.CHARGED;
                this.attackDelay = 20 + entity.getRandom().nextInt(20);
                entity.setChargingCrossbow(false);
            }
        } else if (this.crossbowState == BehaviorCrossbowAttack.BowState.CHARGED) {
            --this.attackDelay;
            if (this.attackDelay == 0) {
                this.crossbowState = BehaviorCrossbowAttack.BowState.READY_TO_ATTACK;
            }
        } else if (this.crossbowState == BehaviorCrossbowAttack.BowState.READY_TO_ATTACK) {
            entity.performRangedAttack(target, 1.0F);
            ItemStack itemStack2 = entity.getItemInHand(ProjectileHelper.getWeaponHoldingHand(entity, Items.CROSSBOW));
            ItemCrossbow.setCharged(itemStack2, false);
            this.crossbowState = BehaviorCrossbowAttack.BowState.UNCHARGED;
        }

    }

    private void lookAtTarget(EntityInsentient entity, EntityLiving target) {
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
    }

    private static EntityLiving getAttackTarget(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    static enum BowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }
}
