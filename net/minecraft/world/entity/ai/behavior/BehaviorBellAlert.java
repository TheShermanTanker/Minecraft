package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class BehaviorBellAlert extends Behavior<EntityLiving> {
    public BehaviorBellAlert() {
        super(ImmutableMap.of(MemoryModuleType.HEARD_BELL_TIME, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Raid raid = world.getRaidAt(entity.getChunkCoordinates());
        if (raid == null) {
            brain.setActiveActivityIfPossible(Activity.HIDE);
        }

    }
}
