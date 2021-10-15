package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.raid.Raid;

public class BehaviorOutsideCelebrate extends BehaviorOutside {
    public BehaviorOutsideCelebrate(float speed) {
        super(speed);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        Raid raid = world.getRaidAt(entity.getChunkCoordinates());
        return raid != null && raid.isVictory() && super.checkExtraStartConditions(world, entity);
    }
}
