package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public abstract class MovingObjectPosition {
    protected final Vec3D location;

    protected MovingObjectPosition(Vec3D pos) {
        this.location = pos;
    }

    public double distanceTo(Entity entity) {
        double d = this.location.x - entity.locX();
        double e = this.location.y - entity.locY();
        double f = this.location.z - entity.locZ();
        return d * d + e * e + f * f;
    }

    public abstract MovingObjectPosition.EnumMovingObjectType getType();

    public Vec3D getPos() {
        return this.location;
    }

    public static enum EnumMovingObjectType {
        MISS,
        BLOCK,
        ENTITY;
    }
}
