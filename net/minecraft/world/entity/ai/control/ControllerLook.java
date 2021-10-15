package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.phys.Vec3D;

public class ControllerLook implements Control {
    protected final EntityInsentient mob;
    protected float yMaxRotSpeed;
    protected float xMaxRotAngle;
    protected boolean hasWanted;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;

    public ControllerLook(EntityInsentient entity) {
        this.mob = entity;
    }

    public void setLookAt(Vec3D direction) {
        this.setLookAt(direction.x, direction.y, direction.z);
    }

    public void setLookAt(Entity entity) {
        this.setLookAt(entity.locX(), getWantedY(entity), entity.locZ());
    }

    public void setLookAt(Entity entity, float yawSpeed, float pitchSpeed) {
        this.setLookAt(entity.locX(), getWantedY(entity), entity.locZ(), yawSpeed, pitchSpeed);
    }

    public void setLookAt(double x, double y, double z) {
        this.setLookAt(x, y, z, (float)this.mob.getHeadRotSpeed(), (float)this.mob.getMaxHeadXRot());
    }

    public void setLookAt(double x, double y, double z, float yawSpeed, float pitchSpeed) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.yMaxRotSpeed = yawSpeed;
        this.xMaxRotAngle = pitchSpeed;
        this.hasWanted = true;
    }

    public void tick() {
        if (this.resetXRotOnTick()) {
            this.mob.setXRot(0.0F);
        }

        if (this.hasWanted) {
            this.hasWanted = false;
            this.getYRotD().ifPresent((float_) -> {
                this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, float_, this.yMaxRotSpeed);
            });
            this.getXRotD().ifPresent((float_) -> {
                this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), float_, this.xMaxRotAngle));
            });
        } else {
            this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
        }

        this.clampHeadRotationToBody();
    }

    protected void clampHeadRotationToBody() {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.yHeadRot = MathHelper.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, (float)this.mob.getMaxHeadYRot());
        }

    }

    protected boolean resetXRotOnTick() {
        return true;
    }

    public boolean isHasWanted() {
        return this.hasWanted;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    protected Optional<Float> getXRotD() {
        double d = this.wantedX - this.mob.locX();
        double e = this.wantedY - this.mob.getHeadY();
        double f = this.wantedZ - this.mob.locZ();
        double g = Math.sqrt(d * d + f * f);
        return !(Math.abs(e) > (double)1.0E-5F) && !(Math.abs(g) > (double)1.0E-5F) ? Optional.empty() : Optional.of((float)(-(MathHelper.atan2(e, g) * (double)(180F / (float)Math.PI))));
    }

    protected Optional<Float> getYRotD() {
        double d = this.wantedX - this.mob.locX();
        double e = this.wantedZ - this.mob.locZ();
        return !(Math.abs(e) > (double)1.0E-5F) && !(Math.abs(d) > (double)1.0E-5F) ? Optional.empty() : Optional.of((float)(MathHelper.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F);
    }

    protected float rotateTowards(float from, float to, float max) {
        float f = MathHelper.degreesDifference(from, to);
        float g = MathHelper.clamp(f, -max, max);
        return from + g;
    }

    private static double getWantedY(Entity entity) {
        return entity instanceof EntityLiving ? entity.getHeadY() : (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0D;
    }
}
