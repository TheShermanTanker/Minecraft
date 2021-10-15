package net.minecraft.world.entity;

import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntitySize {
    public final float width;
    public final float height;
    public final boolean fixed;

    public EntitySize(float width, float height, boolean fixed) {
        this.width = width;
        this.height = height;
        this.fixed = fixed;
    }

    public AxisAlignedBB makeBoundingBox(Vec3D pos) {
        return this.makeBoundingBox(pos.x, pos.y, pos.z);
    }

    public AxisAlignedBB makeBoundingBox(double x, double y, double z) {
        float f = this.width / 2.0F;
        float g = this.height;
        return new AxisAlignedBB(x - (double)f, y, z - (double)f, x + (double)f, y + (double)g, z + (double)f);
    }

    public EntitySize scale(float ratio) {
        return this.scale(ratio, ratio);
    }

    public EntitySize scale(float widthRatio, float heightRatio) {
        return !this.fixed && (widthRatio != 1.0F || heightRatio != 1.0F) ? scalable(this.width * widthRatio, this.height * heightRatio) : this;
    }

    public static EntitySize scalable(float width, float height) {
        return new EntitySize(width, height, false);
    }

    public static EntitySize fixed(float width, float height) {
        return new EntitySize(width, height, true);
    }

    @Override
    public String toString() {
        return "EntityDimensions w=" + this.width + ", h=" + this.height + ", fixed=" + this.fixed;
    }
}
