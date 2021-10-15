package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorGateSingle<E extends EntityLiving> extends BehaviorGate<E> {
    public BehaviorGateSingle(List<Pair<Behavior<? super E>, Integer>> tasks) {
        this(ImmutableMap.of(), tasks);
    }

    public BehaviorGateSingle(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, List<Pair<Behavior<? super E>, Integer>> tasks) {
        super(requiredMemoryState, ImmutableSet.of(), BehaviorGate.Order.SHUFFLED, BehaviorGate.Execution.RUN_ONE, tasks);
    }
}
