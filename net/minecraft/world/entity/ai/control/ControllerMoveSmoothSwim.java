package net.minecraft.world.entity.ai.control;

import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;

public class ControllerMoveSmoothSwim extends ControllerMove {
    private final int maxTurnX;
    private final int maxTurnY;
    private final float inWaterSpeedModifier;
    private final float outsideWaterSpeedModifier;
    private final boolean applyGravity;

    public ControllerMoveSmoothSwim(EntityInsentient entity, int pitchChange, int yawChange, float speedInWater, float speedInAir, boolean buoyant) {
        super(entity);
        this.maxTurnX = pitchChange;
        this.maxTurnY = yawChange;
        this.inWaterSpeedModifier = speedInWater;
        this.outsideWaterSpeedModifier = speedInAir;
        this.applyGravity = buoyant;
    }

    @Override
    public void tick() {
        if (this.applyGravity && this.mob.isInWater()) {
            this.mob.setMot(this.mob.getMot().add(0.0D, 0.005D, 0.0D));
        }

        if (this.operation == ControllerMove.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
            double d = this.wantedX - this.mob.locX();
            double e = this.wantedY - this.mob.locY();
            double f = this.wantedZ - this.mob.locZ();
            double g = d * d + e * e + f * f;
            if (g < (double)2.5000003E-7F) {
                this.mob.setZza(0.0F);
            } else {
                float h = (float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), h, (float)this.maxTurnY));
                this.mob.yBodyRot = this.mob.getYRot();
                this.mob.yHeadRot = this.mob.getYRot();
                float i = (float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                if (this.mob.isInWater()) {
                    this.mob.setSpeed(i * this.inWaterSpeedModifier);
                    double j = Math.sqrt(d * d + f * f);
                    if (Math.abs(e) > (double)1.0E-5F || Math.abs(j) > (double)1.0E-5F) {
                        float k = -((float)(MathHelper.atan2(e, j) * (double)(180F / (float)Math.PI)));
                        k = MathHelper.clamp(MathHelper.wrapDegrees(k), (float)(-this.maxTurnX), (float)this.maxTurnX);
                        this.mob.setXRot(this.rotlerp(this.mob.getXRot(), k, 5.0F));
                    }

                    float l = MathHelper.cos(this.mob.getXRot() * ((float)Math.PI / 180F));
                    float m = MathHelper.sin(this.mob.getXRot() * ((float)Math.PI / 180F));
                    this.mob.zza = l * i;
                    this.mob.yya = -m * i;
                } else {
                    this.mob.setSpeed(i * this.outsideWaterSpeedModifier);
                }

            }
        } else {
            this.mob.setSpeed(0.0F);
            this.mob.setXxa(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }
}
