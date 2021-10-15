package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityCreature;

public class PathfinderGoalWater extends PathfinderGoal {
    private final EntityCreature mob;

    public PathfinderGoalWater(EntityCreature mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return this.mob.isOnGround() && !this.mob.level.getFluid(this.mob.getChunkCoordinates()).is(TagsFluid.WATER);
    }

    @Override
    public void start() {
        BlockPosition blockPos = null;

        for(BlockPosition blockPos2 : BlockPosition.betweenClosed(MathHelper.floor(this.mob.locX() - 2.0D), MathHelper.floor(this.mob.locY() - 2.0D), MathHelper.floor(this.mob.locZ() - 2.0D), MathHelper.floor(this.mob.locX() + 2.0D), this.mob.getBlockY(), MathHelper.floor(this.mob.locZ() + 2.0D))) {
            if (this.mob.level.getFluid(blockPos2).is(TagsFluid.WATER)) {
                blockPos = blockPos2;
                break;
            }
        }

        if (blockPos != null) {
            this.mob.getControllerMove().setWantedPosition((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0D);
        }

    }
}
