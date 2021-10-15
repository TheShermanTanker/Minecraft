package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorRemoveMemory<E extends EntityLiving> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final MemoryModuleType<?> memoryType;

    public BehaviorRemoveMemory(Predicate<E> condition, MemoryModuleType<?> memory) {
        super(ImmutableMap.of(memory, MemoryStatus.VALUE_PRESENT));
        this.predicate = condition;
        this.memoryType = memory;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        entity.getBehaviorController().removeMemory(this.memoryType);
    }
}
