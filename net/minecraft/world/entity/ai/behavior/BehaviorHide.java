package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorHide extends Behavior<EntityLiving> {
    private static final int HIDE_TIMEOUT = 300;
    private final int closeEnoughDist;
    private final int stayHiddenTicks;
    private int ticksHidden;

    public BehaviorHide(int maxHiddenSeconds, int distance) {
        super(ImmutableMap.of(MemoryModuleType.HIDING_PLACE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.HEARD_BELL_TIME, MemoryStatus.VALUE_PRESENT));
        this.stayHiddenTicks = maxHiddenSeconds * 20;
        this.ticksHidden = 0;
        this.closeEnoughDist = distance;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Optional<Long> optional = brain.getMemory(MemoryModuleType.HEARD_BELL_TIME);
        boolean bl = optional.get() + 300L <= time;
        if (this.ticksHidden <= this.stayHiddenTicks && !bl) {
            BlockPosition blockPos = brain.getMemory(MemoryModuleType.HIDING_PLACE).get().getBlockPosition();
            if (blockPos.closerThan(entity.getChunkCoordinates(), (double)this.closeEnoughDist)) {
                ++this.ticksHidden;
            }

        } else {
            brain.removeMemory(MemoryModuleType.HEARD_BELL_TIME);
            brain.removeMemory(MemoryModuleType.HIDING_PLACE);
            brain.updateActivityFromSchedule(world.getDayTime(), world.getTime());
            this.ticksHidden = 0;
        }
    }
}
