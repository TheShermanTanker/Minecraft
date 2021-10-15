package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathPoint;

public class BehaviorSleep extends Behavior<EntityLiving> {
    public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
    private long nextOkStartTime;

    public BehaviorSleep() {
        super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        if (entity.isPassenger()) {
            return false;
        } else {
            BehaviorController<?> brain = entity.getBehaviorController();
            GlobalPos globalPos = brain.getMemory(MemoryModuleType.HOME).get();
            if (world.getDimensionKey() != globalPos.getDimensionManager()) {
                return false;
            } else {
                Optional<Long> optional = brain.getMemory(MemoryModuleType.LAST_WOKEN);
                if (optional.isPresent()) {
                    long l = world.getTime() - optional.get();
                    if (l > 0L && l < 100L) {
                        return false;
                    }
                }

                IBlockData blockState = world.getType(globalPos.getBlockPosition());
                return globalPos.getBlockPosition().closerThan(entity.getPositionVector(), 2.0D) && blockState.is(TagsBlock.BEDS) && !blockState.get(BlockBed.OCCUPIED);
            }
        }
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityLiving entity, long time) {
        Optional<GlobalPos> optional = entity.getBehaviorController().getMemory(MemoryModuleType.HOME);
        if (!optional.isPresent()) {
            return false;
        } else {
            BlockPosition blockPos = optional.get().getBlockPosition();
            return entity.getBehaviorController().isActive(Activity.REST) && entity.locY() > (double)blockPos.getY() + 0.4D && blockPos.closerThan(entity.getPositionVector(), 1.14D);
        }
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        if (time > this.nextOkStartTime) {
            BehaviorInteractDoor.closeDoorsThatIHaveOpenedOrPassedThrough(world, entity, (PathPoint)null, (PathPoint)null);
            entity.entitySleep(entity.getBehaviorController().getMemory(MemoryModuleType.HOME).get().getBlockPosition());
        }

    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void stop(WorldServer world, EntityLiving entity, long time) {
        if (entity.isSleeping()) {
            entity.entityWakeup();
            this.nextOkStartTime = time + 40L;
        }

    }
}
