package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.GameRules;

public class BehaviorForgetAnger<E extends EntityInsentient> extends Behavior<E> {
    public BehaviorForgetAnger() {
        super(ImmutableMap.of(MemoryModuleType.ANGRY_AT, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorUtil.getLivingEntityFromUUIDMemory(entity, MemoryModuleType.ANGRY_AT).ifPresent((livingEntity) -> {
            if (livingEntity.isDeadOrDying() && (livingEntity.getEntityType() != EntityTypes.PLAYER || world.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS))) {
                entity.getBehaviorController().removeMemory(MemoryModuleType.ANGRY_AT);
            }

        });
    }
}
