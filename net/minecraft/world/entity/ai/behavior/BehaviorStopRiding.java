package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorStopRiding<E extends EntityLiving, T extends Entity> extends Behavior<E> {
    private final int maxWalkDistToRideTarget;
    private final BiPredicate<E, Entity> dontRideIf;

    public BehaviorStopRiding(int range, BiPredicate<E, Entity> alternativeRideCondition) {
        super(ImmutableMap.of(MemoryModuleType.RIDE_TARGET, MemoryStatus.REGISTERED));
        this.maxWalkDistToRideTarget = range;
        this.dontRideIf = alternativeRideCondition;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        Entity entity2 = entity.getVehicle();
        Entity entity3 = entity.getBehaviorController().getMemory(MemoryModuleType.RIDE_TARGET).orElse((Entity)null);
        if (entity2 == null && entity3 == null) {
            return false;
        } else {
            Entity entity4 = entity2 == null ? entity3 : entity2;
            return !this.isVehicleValid(entity, entity4) || this.dontRideIf.test(entity, entity4);
        }
    }

    private boolean isVehicleValid(E entity, Entity target) {
        return target.isAlive() && target.closerThan(entity, (double)this.maxWalkDistToRideTarget) && target.level == entity.level;
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        entity.stopRiding();
        entity.getBehaviorController().removeMemory(MemoryModuleType.RIDE_TARGET);
    }
}
