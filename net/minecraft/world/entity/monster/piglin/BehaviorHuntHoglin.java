package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;

public class BehaviorHuntHoglin<E extends EntityPiglin> extends Behavior<E> {
    public BehaviorHuntHoglin() {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HUNTED_RECENTLY, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityPiglin entity) {
        return !entity.isBaby() && !PiglinAI.hasAnyoneNearbyHuntedRecently(entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        EntityHoglin hoglin = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN).get();
        PiglinAI.setAngerTarget(entity, hoglin);
        PiglinAI.dontKillAnyMoreHoglinsForAWhile(entity);
        PiglinAI.broadcastAngerTarget(entity, hoglin);
        PiglinAI.broadcastDontKillAnyMoreHoglinsForAWhile(entity);
    }
}
