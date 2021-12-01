package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorGate<E extends EntityLiving> extends Behavior<E> {
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final BehaviorGate.Order orderPolicy;
    private final BehaviorGate.Execution runningPolicy;
    private final ShufflingList<Behavior<? super E>> behaviors = new ShufflingList<>();

    public BehaviorGate(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, Set<MemoryModuleType<?>> memoriesToForgetWhenStopped, BehaviorGate.Order order, BehaviorGate.Execution runMode, List<Pair<Behavior<? super E>, Integer>> tasks) {
        super(requiredMemoryState);
        this.exitErasedMemories = memoriesToForgetWhenStopped;
        this.orderPolicy = order;
        this.runningPolicy = runMode;
        tasks.forEach((pair) -> {
            this.behaviors.add(pair.getFirst(), pair.getSecond());
        });
    }

    @Override
    protected boolean canStillUse(WorldServer world, E entity, long time) {
        return this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).anyMatch((task) -> {
            return task.canStillUse(world, entity, time);
        });
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        this.orderPolicy.apply(this.behaviors);
        this.runningPolicy.apply(this.behaviors.stream(), world, entity, time);
    }

    @Override
    protected void tick(WorldServer world, E entity, long time) {
        this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).forEach((task) -> {
            task.tickOrStop(world, entity, time);
        });
    }

    @Override
    protected void stop(WorldServer world, E entity, long time) {
        this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).forEach((task) -> {
            task.doStop(world, entity, time);
        });
        this.exitErasedMemories.forEach(entity.getBehaviorController()::removeMemory);
    }

    @Override
    public String toString() {
        Set<? extends Behavior<? super E>> set = this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).collect(Collectors.toSet());
        return "(" + this.getClass().getSimpleName() + "): " + set;
    }

    public static enum Execution {
        RUN_ONE {
            @Override
            public <E extends EntityLiving> void apply(Stream<Behavior<? super E>> tasks, WorldServer world, E entity, long time) {
                tasks.filter((task) -> {
                    return task.getStatus() == Behavior.Status.STOPPED;
                }).filter((task) -> {
                    return task.tryStart(world, entity, time);
                }).findFirst();
            }
        },
        TRY_ALL {
            @Override
            public <E extends EntityLiving> void apply(Stream<Behavior<? super E>> tasks, WorldServer world, E entity, long time) {
                tasks.filter((task) -> {
                    return task.getStatus() == Behavior.Status.STOPPED;
                }).forEach((task) -> {
                    task.tryStart(world, entity, time);
                });
            }
        };

        public abstract <E extends EntityLiving> void apply(Stream<Behavior<? super E>> tasks, WorldServer world, E entity, long time);
    }

    public static enum Order {
        ORDERED((shufflingList) -> {
        }),
        SHUFFLED(ShufflingList::shuffle);

        private final Consumer<ShufflingList<?>> consumer;

        private Order(Consumer<ShufflingList<?>> listModifier) {
            this.consumer = listModifier;
        }

        public void apply(ShufflingList<?> list) {
            this.consumer.accept(list);
        }
    }
}
