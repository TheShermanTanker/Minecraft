package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorAdmireTimeout<E extends EntityPiglin> extends Behavior<E> {
    private final int maxTimeToReachItem;
    private final int disableTime;

    public BehaviorAdmireTimeout(int timeLimit, int cooldown) {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, MemoryStatus.REGISTERED, MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, MemoryStatus.REGISTERED));
        this.maxTimeToReachItem = timeLimit;
        this.disableTime = cooldown;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return entity.getItemInOffHand().isEmpty();
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorController<EntityPiglin> brain = entity.getBehaviorController();
        Optional<Integer> optional = brain.getMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
        if (!optional.isPresent()) {
            brain.setMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, 0);
        } else {
            int i = optional.get();
            if (i > this.maxTimeToReachItem) {
                brain.removeMemory(MemoryModuleType.ADMIRING_ITEM);
                brain.removeMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
                brain.setMemoryWithExpiry(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, true, (long)this.disableTime);
            } else {
                brain.setMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, i + 1);
            }
        }

    }
}
