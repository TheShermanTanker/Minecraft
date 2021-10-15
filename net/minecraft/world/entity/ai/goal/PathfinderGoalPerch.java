package net.minecraft.world.entity.ai.goal;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.animal.EntityPerchable;

public class PathfinderGoalPerch extends PathfinderGoal {
    private final EntityPerchable entity;
    private EntityPlayer owner;
    private boolean isSittingOnShoulder;

    public PathfinderGoalPerch(EntityPerchable tameable) {
        this.entity = tameable;
    }

    @Override
    public boolean canUse() {
        EntityPlayer serverPlayer = (EntityPlayer)this.entity.getOwner();
        boolean bl = serverPlayer != null && !serverPlayer.isSpectator() && !serverPlayer.getAbilities().flying && !serverPlayer.isInWater() && !serverPlayer.isInPowderSnow;
        return !this.entity.isWillSit() && bl && this.entity.canSitOnShoulder();
    }

    @Override
    public boolean isInterruptable() {
        return !this.isSittingOnShoulder;
    }

    @Override
    public void start() {
        this.owner = (EntityPlayer)this.entity.getOwner();
        this.isSittingOnShoulder = false;
    }

    @Override
    public void tick() {
        if (!this.isSittingOnShoulder && !this.entity.isSitting() && !this.entity.isLeashed()) {
            if (this.entity.getBoundingBox().intersects(this.owner.getBoundingBox())) {
                this.isSittingOnShoulder = this.entity.setEntityOnShoulder(this.owner);
            }

        }
    }
}
