package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class PlayDead extends Behavior<EntityAxolotl> {
    public PlayDead() {
        super(ImmutableMap.of(MemoryModuleType.PLAY_DEAD_TICKS, MemoryStatus.VALUE_PRESENT, MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityAxolotl entity) {
        return entity.isInWaterOrBubble();
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityAxolotl axolotl, long l) {
        return axolotl.isInWaterOrBubble() && axolotl.getBehaviorController().hasMemory(MemoryModuleType.PLAY_DEAD_TICKS);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityAxolotl axolotl, long l) {
        BehaviorController<EntityAxolotl> brain = axolotl.getBehaviorController();
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        brain.removeMemory(MemoryModuleType.LOOK_TARGET);
        axolotl.addEffect(new MobEffect(MobEffects.REGENERATION, 200, 0));
    }
}
