package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.animal.EntityDolphin;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalWaterJump extends PathfinderGoalWaterJumpAbstract {
    private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
    private final EntityDolphin dolphin;
    private final int interval;
    private boolean breached;

    public PathfinderGoalWaterJump(EntityDolphin dolphin, int chance) {
        this.dolphin = dolphin;
        this.interval = chance;
    }

    @Override
    public boolean canUse() {
        if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
            return false;
        } else {
            EnumDirection direction = this.dolphin.getAdjustedDirection();
            int i = direction.getAdjacentX();
            int j = direction.getAdjacentZ();
            BlockPosition blockPos = this.dolphin.getChunkCoordinates();

            for(int k : STEPS_TO_CHECK) {
                if (!this.waterIsClear(blockPos, i, j, k) || !this.surfaceIsClear(blockPos, i, j, k)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean waterIsClear(BlockPosition pos, int offsetX, int offsetZ, int multiplier) {
        BlockPosition blockPos = pos.offset(offsetX * multiplier, 0, offsetZ * multiplier);
        return this.dolphin.level.getFluid(blockPos).is(TagsFluid.WATER) && !this.dolphin.level.getType(blockPos).getMaterial().isSolid();
    }

    private boolean surfaceIsClear(BlockPosition pos, int offsetX, int offsetZ, int multiplier) {
        return this.dolphin.level.getType(pos.offset(offsetX * multiplier, 1, offsetZ * multiplier)).isAir() && this.dolphin.level.getType(pos.offset(offsetX * multiplier, 2, offsetZ * multiplier)).isAir();
    }

    @Override
    public boolean canContinueToUse() {
        double d = this.dolphin.getMot().y;
        return (!(d * d < (double)0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater()) && !this.dolphin.isOnGround();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        EnumDirection direction = this.dolphin.getAdjustedDirection();
        this.dolphin.setMot(this.dolphin.getMot().add((double)direction.getAdjacentX() * 0.6D, 0.7D, (double)direction.getAdjacentZ() * 0.6D));
        this.dolphin.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.dolphin.setXRot(0.0F);
    }

    @Override
    public void tick() {
        boolean bl = this.breached;
        if (!bl) {
            Fluid fluidState = this.dolphin.level.getFluid(this.dolphin.getChunkCoordinates());
            this.breached = fluidState.is(TagsFluid.WATER);
        }

        if (this.breached && !bl) {
            this.dolphin.playSound(SoundEffects.DOLPHIN_JUMP, 1.0F, 1.0F);
        }

        Vec3D vec3 = this.dolphin.getMot();
        if (vec3.y * vec3.y < (double)0.03F && this.dolphin.getXRot() != 0.0F) {
            this.dolphin.setXRot(MathHelper.rotlerp(this.dolphin.getXRot(), 0.0F, 0.2F));
        } else if (vec3.length() > (double)1.0E-5F) {
            double d = vec3.horizontalDistance();
            double e = Math.atan2(-vec3.y, d) * (double)(180F / (float)Math.PI);
            this.dolphin.setXRot((float)e);
        }

    }
}
