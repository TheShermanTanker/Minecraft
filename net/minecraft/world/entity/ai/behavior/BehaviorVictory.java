package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.raid.Raid;

public class BehaviorVictory extends BehaviorStrollRandom {
    public BehaviorVictory(float walkSpeed) {
        super(walkSpeed);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        Raid raid = world.getRaidAt(entity.getChunkCoordinates());
        return raid != null && raid.isVictory() && super.checkExtraStartConditions(world, entity);
    }
}
