package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemProjectileWeapon;

public class BehaviorAttack extends Behavior<EntityInsentient> {
    private final int cooldownBetweenAttacks;

    public BehaviorAttack(int interval) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT));
        this.cooldownBetweenAttacks = interval;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityInsentient entity) {
        EntityLiving livingEntity = this.getAttackTarget(entity);
        return !this.isHoldingUsableProjectileWeapon(entity) && BehaviorUtil.canSee(entity, livingEntity) && BehaviorUtil.isWithinMeleeAttackRange(entity, livingEntity);
    }

    private boolean isHoldingUsableProjectileWeapon(EntityInsentient entity) {
        return entity.isHolding((stack) -> {
            Item item = stack.getItem();
            return item instanceof ItemProjectileWeapon && entity.canFireProjectileWeapon((ItemProjectileWeapon)item);
        });
    }

    @Override
    protected void start(WorldServer world, EntityInsentient entity, long time) {
        EntityLiving livingEntity = this.getAttackTarget(entity);
        BehaviorUtil.lookAtEntity(entity, livingEntity);
        entity.swingHand(EnumHand.MAIN_HAND);
        entity.attackEntity(livingEntity);
        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, (long)this.cooldownBetweenAttacks);
    }

    private EntityLiving getAttackTarget(EntityInsentient entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
