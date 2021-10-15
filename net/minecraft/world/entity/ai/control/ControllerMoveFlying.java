package net.minecraft.world.entity.ai.control;

import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;

public class ControllerMoveFlying extends ControllerMove {
    private final int maxTurn;
    private final boolean hoversInPlace;

    public ControllerMoveFlying(EntityInsentient entity, int maxPitchChange, boolean noGravity) {
        super(entity);
        this.maxTurn = maxPitchChange;
        this.hoversInPlace = noGravity;
    }

    @Override
    public void tick() {
        if (this.operation == ControllerMove.Operation.MOVE_TO) {
            this.operation = ControllerMove.Operation.WAIT;
            this.mob.setNoGravity(true);
            double d = this.wantedX - this.mob.locX();
            double e = this.wantedY - this.mob.locY();
            double f = this.wantedZ - this.mob.locZ();
            double g = d * d + e * e + f * f;
            if (g < (double)2.5000003E-7F) {
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
                return;
            }

            float h = (float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), h, 90.0F));
            float i;
            if (this.mob.isOnGround()) {
                i = (float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
            } else {
                i = (float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.FLYING_SPEED));
            }

            this.mob.setSpeed(i);
            double k = Math.sqrt(d * d + f * f);
            if (Math.abs(e) > (double)1.0E-5F || Math.abs(k) > (double)1.0E-5F) {
                float l = (float)(-(MathHelper.atan2(e, k) * (double)(180F / (float)Math.PI)));
                this.mob.setXRot(this.rotlerp(this.mob.getXRot(), l, (float)this.maxTurn));
                this.mob.setYya(e > 0.0D ? i : -i);
            }
        } else {
            if (!this.hoversInPlace) {
                this.mob.setNoGravity(false);
            }

            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }

    }
}
