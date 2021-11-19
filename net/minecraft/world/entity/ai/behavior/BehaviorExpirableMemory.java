package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorExpirableMemory<E extends EntityInsentient, T> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final MemoryModuleType<? extends T> sourceMemory;
    private final MemoryModuleType<T> targetMemory;
    private final IntProviderUniform durationOfCopy;

    public BehaviorExpirableMemory(Predicate<E> runPredicate, MemoryModuleType<? extends T> sourceType, MemoryModuleType<T> targetType, IntProviderUniform duration) {
        super(ImmutableMap.of(sourceType, MemoryStatus.VALUE_PRESENT, targetType, MemoryStatus.VALUE_ABSENT));
        this.predicate = runPredicate;
        this.sourceMemory = sourceType;
        this.targetMemory = targetType;
        this.durationOfCopy = duration;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemoryWithExpiry(this.targetMemory, brain.getMemory(this.sourceMemory).get(), (long)this.durationOfCopy.sample(world.random));
    }
}
