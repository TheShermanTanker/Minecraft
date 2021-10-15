package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public abstract class Behavior<E extends EntityLiving> {
    private static final int DEFAULT_DURATION = 60;
    protected final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private Behavior.Status status = Behavior.Status.STOPPED;
    private long endTimestamp;
    private final int minDuration;
    private final int maxDuration;

    public Behavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState) {
        this(requiredMemoryState, 60);
    }

    public Behavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int runTime) {
        this(requiredMemoryState, runTime, runTime);
    }

    public Behavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int minRunTime, int maxRunTime) {
        this.minDuration = minRunTime;
        this.maxDuration = maxRunTime;
        this.entryCondition = requiredMemoryState;
    }

    public Behavior.Status getStatus() {
        return this.status;
    }

    public final boolean tryStart(WorldServer world, E entity, long time) {
        if (this.hasRequiredMemories(entity) && this.checkExtraStartConditions(world, entity)) {
            this.status = Behavior.Status.RUNNING;
            int i = this.minDuration + world.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
            this.endTimestamp = time + (long)i;
            this.start(world, entity, time);
            return true;
        } else {
            return false;
        }
    }

    protected void start(WorldServer world, E entity, long time) {
    }

    public final void tickOrStop(WorldServer world, E entity, long time) {
        if (!this.timedOut(time) && this.canStillUse(world, entity, time)) {
            this.tick(world, entity, time);
        } else {
            this.doStop(world, entity, time);
        }

    }

    protected void tick(WorldServer world, E entity, long time) {
    }

    public final void doStop(WorldServer world, E entity, long time) {
        this.status = Behavior.Status.STOPPED;
        this.stop(world, entity, time);
    }

    protected void stop(WorldServer world, E entity, long time) {
    }

    protected boolean canStillUse(WorldServer world, E entity, long time) {
        return false;
    }

    protected boolean timedOut(long time) {
        return time > this.endTimestamp;
    }

    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    private boolean hasRequiredMemories(E entity) {
        for(Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
            MemoryModuleType<?> memoryModuleType = entry.getKey();
            MemoryStatus memoryStatus = entry.getValue();
            if (!entity.getBehaviorController().checkMemory(memoryModuleType, memoryStatus)) {
                return false;
            }
        }

        return true;
    }

    public static enum Status {
        STOPPED,
        RUNNING;
    }
}
