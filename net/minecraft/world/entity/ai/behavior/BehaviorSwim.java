package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.EntityInsentient;

public class BehaviorSwim extends Behavior<EntityInsentient> {
    private final float chance;

    public BehaviorSwim(float chance) {
        super(ImmutableMap.of());
        this.chance = chance;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityInsentient entity) {
        return entity.isInWater() && entity.getFluidHeight(TagsFluid.WATER) > entity.getFluidJumpThreshold() || entity.isInLava();
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        return this.checkExtraStartConditions(serverLevel, mob);
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityInsentient mob, long l) {
        if (mob.getRandom().nextFloat() < this.chance) {
            mob.getControllerJump().jump();
        }

    }
}
