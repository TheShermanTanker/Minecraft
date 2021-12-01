package net.minecraft.world.entity.ai.control;

import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;

public class ControllerLookSmoothSwim extends ControllerLook {
    private final int maxYRotFromCenter;
    private static final int HEAD_TILT_X = 10;
    private static final int HEAD_TILT_Y = 20;

    public ControllerLookSmoothSwim(EntityInsentient entity, int yawAdjustThreshold) {
        super(entity);
        this.maxYRotFromCenter = yawAdjustThreshold;
    }

    @Override
    public void tick() {
        if (this.lookAtCooldown > 0) {
            --this.lookAtCooldown;
            this.getYRotD().ifPresent((yaw) -> {
                this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, yaw + 20.0F, this.yMaxRotSpeed);
            });
            this.getXRotD().ifPresent((pitch) -> {
                this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), pitch + 10.0F, this.xMaxRotAngle));
            });
        } else {
            if (this.mob.getNavigation().isDone()) {
                this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), 0.0F, 5.0F));
            }

            this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, this.yMaxRotSpeed);
        }

        float f = MathHelper.wrapDegrees(this.mob.yHeadRot - this.mob.yBodyRot);
        if (f < (float)(-this.maxYRotFromCenter)) {
            this.mob.yBodyRot -= 4.0F;
        } else if (f > (float)this.maxYRotFromCenter) {
            this.mob.yBodyRot += 4.0F;
        }

    }
}
