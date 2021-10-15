package net.minecraft.world.level.portal;

import net.minecraft.world.phys.Vec3D;

public class ShapeDetectorShape {
    public final Vec3D pos;
    public final Vec3D speed;
    public final float yRot;
    public final float xRot;

    public ShapeDetectorShape(Vec3D position, Vec3D velocity, float yaw, float pitch) {
        this.pos = position;
        this.speed = velocity;
        this.yRot = yaw;
        this.xRot = pitch;
    }
}
