package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class ValidatePlayDead extends Behavior<EntityAxolotl> {
    public ValidatePlayDead() {
        super(ImmutableMap.of(MemoryModuleType.PLAY_DEAD_TICKS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected void start(WorldServer world, EntityAxolotl entity, long time) {
        BehaviorController<EntityAxolotl> brain = entity.getBehaviorController();
        int i = brain.getMemory(MemoryModuleType.PLAY_DEAD_TICKS).get();
        if (i <= 0) {
            brain.removeMemory(MemoryModuleType.PLAY_DEAD_TICKS);
            brain.removeMemory(MemoryModuleType.HURT_BY_ENTITY);
            brain.useDefaultActivity();
        } else {
            brain.setMemory(MemoryModuleType.PLAY_DEAD_TICKS, i - 1);
        }

    }
}
