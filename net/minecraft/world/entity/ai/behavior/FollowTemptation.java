package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.SystemUtils;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.player.EntityHuman;

public class FollowTemptation extends Behavior<EntityCreature> {
    public static final int TEMPTATION_COOLDOWN = 100;
    public static final double CLOSE_ENOUGH_DIST = 2.5D;
    private final Function<EntityLiving, Float> speedModifier;

    public FollowTemptation(Function<EntityLiving, Float> speed) {
        super(SystemUtils.make(() -> {
            Builder<MemoryModuleType<?>, MemoryStatus> builder = ImmutableMap.builder();
            builder.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
            builder.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
            return builder.build();
        }));
        this.speedModifier = speed;
    }

    protected float getSpeedModifier(EntityCreature entity) {
        return this.speedModifier.apply(entity);
    }

    private Optional<EntityHuman> getTemptingPlayer(EntityCreature entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.TEMPTING_PLAYER);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        return this.getTemptingPlayer(pathfinderMob).isPresent() && !pathfinderMob.getBehaviorController().hasMemory(MemoryModuleType.BREED_TARGET);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        pathfinderMob.getBehaviorController().setMemory(MemoryModuleType.IS_TEMPTED, true);
    }

    @Override
    protected void stop(WorldServer world, EntityCreature entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
        brain.setMemory(MemoryModuleType.IS_TEMPTED, false);
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        brain.removeMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(WorldServer world, EntityCreature entity, long time) {
        EntityHuman player = this.getTemptingPlayer(entity).get();
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(player, true));
        if (entity.distanceToSqr(player) < 6.25D) {
            brain.removeMemory(MemoryModuleType.WALK_TARGET);
        } else {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(player, false), this.getSpeedModifier(entity), 2));
        }

    }
}
