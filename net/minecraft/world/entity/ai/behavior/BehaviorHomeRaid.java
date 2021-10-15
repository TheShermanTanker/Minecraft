package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.raid.Raid;

public class BehaviorHomeRaid extends BehaviorHome {
    public BehaviorHomeRaid(int maxDistance, float walkSpeed) {
        super(maxDistance, walkSpeed, 1);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        Raid raid = world.getRaidAt(entity.getChunkCoordinates());
        return super.checkExtraStartConditions(world, entity) && raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
    }
}
