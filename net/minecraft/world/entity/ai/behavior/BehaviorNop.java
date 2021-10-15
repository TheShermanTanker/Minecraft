package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;

public class BehaviorNop extends Behavior<EntityLiving> {
    public BehaviorNop(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(), minRunTime, maxRunTime);
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityLiving entity, long time) {
        return true;
    }
}
