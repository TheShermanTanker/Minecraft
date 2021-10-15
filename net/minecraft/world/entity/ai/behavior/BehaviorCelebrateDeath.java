package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.GameRules;

public class BehaviorCelebrateDeath extends Behavior<EntityLiving> {
    private final int celebrateDuration;
    private final BiPredicate<EntityLiving, EntityLiving> dancePredicate;

    public BehaviorCelebrateDeath(int duration, BiPredicate<EntityLiving, EntityLiving> predicate) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED, MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_ABSENT, MemoryModuleType.DANCING, MemoryStatus.REGISTERED));
        this.celebrateDuration = duration;
        this.dancePredicate = predicate;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return this.getAttackTarget(entity).isDeadOrDying();
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        EntityLiving livingEntity = this.getAttackTarget(entity);
        if (this.dancePredicate.test(entity, livingEntity)) {
            entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.DANCING, true, (long)this.celebrateDuration);
        }

        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.CELEBRATE_LOCATION, livingEntity.getChunkCoordinates(), (long)this.celebrateDuration);
        if (livingEntity.getEntityType() != EntityTypes.PLAYER || world.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            entity.getBehaviorController().removeMemory(MemoryModuleType.ATTACK_TARGET);
            entity.getBehaviorController().removeMemory(MemoryModuleType.ANGRY_AT);
        }

    }

    private EntityLiving getAttackTarget(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
